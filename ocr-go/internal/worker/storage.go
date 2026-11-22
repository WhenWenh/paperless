package worker

import (
	"context"
	"errors"
	"fmt"
	"io"
	"net/url"
	"os"
	"path/filepath"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/s3/types"
)

const storageDirectory = "/tmp/paperless"

func openStorage(ctx context.Context, config Config) (func(context.Context, string) (io.ReadCloser, error), error) {
	switch config.StorageType {
	case "filesystem":
		return func(_ context.Context, storageUUID string) (io.ReadCloser, error) {
			return os.Open(filepath.Join(storageDirectory, storageUUID))
		}, nil
	case "s3":
		if config.StorageAccessKey == "" || config.StorageSecretKey == "" {
			return nil, errors.New("STORAGE_ACCESS_KEY and STORAGE_SECRET_KEY are required for S3 storage")
		}
		endpoint, err := url.Parse(config.StorageEndpoint)
		if err != nil || endpoint.Host == "" || (endpoint.Scheme != "http" && endpoint.Scheme != "https") {
			return nil, errors.New("STORAGE_ENDPOINT must be an HTTP or HTTPS URL")
		}
		client := s3.New(s3.Options{
			BaseEndpoint: aws.String(config.StorageEndpoint),
			Region:       config.StorageRegion,
			Credentials:  credentials.NewStaticCredentialsProvider(config.StorageAccessKey, config.StorageSecretKey, ""),
			UsePathStyle: true,
		})
		bucket := aws.String(config.StorageBucket)
		if err := ensureBucket(ctx, client, bucket, config.StorageRegion); err != nil {
			return nil, err
		}
		return func(ctx context.Context, storageUUID string) (io.ReadCloser, error) {
			result, err := client.GetObject(ctx, &s3.GetObjectInput{
				Bucket: bucket,
				Key:    aws.String(storageUUID),
			})
			if err != nil {
				return nil, err
			}
			return result.Body, nil
		}, nil
	default:
		return nil, fmt.Errorf("unsupported STORAGE_TYPE %q", config.StorageType)
	}
}

func ensureBucket(ctx context.Context, client *s3.Client, bucket *string, region string) error {
	head := &s3.HeadBucketInput{Bucket: bucket}
	if _, err := client.HeadBucket(ctx, head); err != nil {
		var response interface{ HTTPStatusCode() int }
		if !errors.As(err, &response) || response.HTTPStatusCode() != 404 {
			return fmt.Errorf("check S3 bucket: %w", err)
		}

		create := &s3.CreateBucketInput{Bucket: bucket}
		if region != "us-east-1" {
			create.CreateBucketConfiguration = &types.CreateBucketConfiguration{
				LocationConstraint: types.BucketLocationConstraint(region),
			}
		}
		if _, err := client.CreateBucket(ctx, create); err != nil {
			// The API can create the same bucket while the OCR worker is starting.
			if !errors.As(err, &response) || response.HTTPStatusCode() != 409 {
				return fmt.Errorf("create S3 bucket: %w", err)
			}
			if _, err := client.HeadBucket(ctx, head); err != nil {
				return fmt.Errorf("check S3 bucket after concurrent creation: %w", err)
			}
		}
	}
	return nil
}
