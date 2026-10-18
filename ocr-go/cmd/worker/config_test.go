package main

import (
 "net/url"
 "testing"
)

func TestLoadConfig(t *testing.T) {
 for _, tc := range []struct{name, host, port, user, password string; invalid bool}{
  {"defaults", "", "", "user", "secret", false},
  {"overrides", "broker", "5673", "user", "secret", false},
  {"missing user", "", "", "", "secret", true},
  {"missing password", "", "", "user", "", true},
 } { t.Run(tc.name, func(t *testing.T) {
  t.Setenv("RABBITMQ_HOST", tc.host); t.Setenv("RABBITMQ_PORT", tc.port)
  t.Setenv("RABBITMQ_USER", tc.user); t.Setenv("RABBITMQ_PASSWORD", tc.password)
  cfg, err := loadConfig()
  if tc.invalid { if err == nil {t.Fatal("missing credentials accepted")}; return }
  if err != nil {t.Fatal(err)}
  host, port := tc.host, tc.port; if host == "" {host="localhost"}; if port=="" {port="5672"}
  if cfg.RabbitHost!=host || cfg.RabbitPort!=port || cfg.RabbitUser!=tc.user || cfg.RabbitPassword!=tc.password || cfg.HealthAddress!=":8082" {t.Fatalf("configuration fields differ")}
 }) }
}

func TestRabbitURLPreservesCredentialsAndIPv6(t *testing.T) {
 cfg:=Config{RabbitHost:"::1",RabbitPort:"5672",RabbitUser:"u@ser",RabbitPassword:"p:/@?#%"}
 u,err:=url.Parse(cfg.rabbitURL()); if err!=nil {t.Fatal(err)}
 password,_:=u.User.Password()
 if u.Scheme!="amqp" || u.Host!="[::1]:5672" || u.Path!="/" || u.User.Username()!=cfg.RabbitUser || password!=cfg.RabbitPassword {t.Fatal("URL did not preserve configuration")}
}
