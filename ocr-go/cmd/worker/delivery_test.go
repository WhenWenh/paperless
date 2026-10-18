package main

import (
 "context"
 "errors"
 "testing"
 amqp "github.com/rabbitmq/amqp091-go"
)

const validBody=`{"documentId":"217df5bc-661c-4c0b-843f-93bdd1273e94","storageUuid":"1e195006-626f-4437-b913-9b8358607ef9","contentType":"application/pdf"}`

type recordingAcknowledger struct {acks,nacks int;tag uint64;multiple,requeue bool;ackErr,nackErr error;onAck func()}
func(a *recordingAcknowledger)Ack(tag uint64,multiple bool)error{a.acks++;a.tag=tag;a.multiple=multiple;if a.onAck!=nil{a.onAck()};return a.ackErr}
func(a *recordingAcknowledger)Nack(tag uint64,multiple,requeue bool)error{a.nacks++;a.tag=tag;a.multiple=multiple;a.requeue=requeue;return a.nackErr}
func(a *recordingAcknowledger)Reject(uint64,bool)error{return errors.New("unexpected Reject call")}

func TestDecodeAdditionalInvalidMessages(t *testing.T){
 for _,body:=range []string{`{"documentId":"id","contentType":"application/pdf"}`,`{"documentId":"id","storageUuid":"file","contentType":"image/png"}`,`{"documentId":"id","storageUuid":"file"}`} {
  if _,err:=decodeMessage([]byte(body));err==nil{t.Fatalf("accepted invalid body: %s",body)}
 }
}

func TestHandleDeliveryAcknowledgements(t *testing.T){
 failure:=errors.New("connection closed")
 for _,tc:=range []struct{name,body string;ackErr,nackErr error;cancel bool}{
  {name:"success",body:validBody},
  {name:"ack failure",body:validBody,ackErr:failure},
  {name:"invalid message requeued",body:`{`},
  {name:"nack failure",body:`{`,nackErr:failure},
  {name:"shutdown during retry",body:`{`,cancel:true},
 } {t.Run(tc.name,func(t *testing.T){
  ctx,cancel:=context.WithCancel(context.Background());defer cancel();if tc.cancel{cancel()}
  ack:=&recordingAcknowledger{ackErr:tc.ackErr,nackErr:tc.nackErr}
  err:=handleDelivery(ctx,amqp.Delivery{Body:[]byte(tc.body),DeliveryTag:42,Acknowledger:ack})
  if tc.cancel{if !errors.Is(err,context.Canceled)||ack.acks!=0||ack.nacks!=0{t.Fatalf("cancellation: %v %+v",err,ack)};return}
  wantErr:=tc.ackErr;if wantErr==nil{wantErr=tc.nackErr};if !errors.Is(err,wantErr){t.Fatalf("error: %v, want %v",err,wantErr)}
  if tc.body==validBody{if ack.acks!=1||ack.nacks!=0{t.Fatal("successful processing must only ACK")}}else{if ack.acks!=0||ack.nacks!=1||!ack.requeue{t.Fatal("invalid processing must only requeue")}}
  if ack.tag!=42||ack.multiple{t.Fatal("must acknowledge only current delivery")}
 })}
}
