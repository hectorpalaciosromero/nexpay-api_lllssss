#!/bin/bash
# LocalStack init script - creates AWS resources for NexPay

echo "Initializing AWS resources..."

REGION="us-east-1"
ENDPOINT="http://localhost:4566"

# SQS Queues

echo "Creating SQS queues..."

# DLQ must be created first
awslocal sqs create-queue \
    --queue-name payment-events-dlq \
    --attributes '{
        "MessageRetentionPeriod": "1209600"
    }'

PAYMENT_DLQ_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/payment-events-dlq \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)

awslocal sqs create-queue \
    --queue-name payment-events \
    --attributes '{
        "VisibilityTimeout": "30",
        "MessageRetentionPeriod": "86400",
        "RedrivePolicy": "{\"deadLetterTargetArn\":\"'"$PAYMENT_DLQ_ARN"'\",\"maxReceiveCount\":\"3\"}"
    }'

echo "  payment-events queue created (with DLQ)"

# SNS Topics

echo "Creating SNS topics..."

awslocal sns create-topic --name payment-notifications
awslocal sns create-topic --name user-alerts

# Subscribe SQS to SNS for fan-out
PAYMENT_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/payment-events \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)

PAYMENT_TOPIC_ARN=$(awslocal sns list-topics \
    --query 'Topics[?ends_with(TopicArn, `payment-notifications`)].TopicArn' \
    --output text)

awslocal sns subscribe \
    --topic-arn "$PAYMENT_TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$PAYMENT_QUEUE_ARN"

echo "  SQS subscribed to SNS topic"

# S3 Buckets

echo "Creating S3 buckets..."

awslocal s3 mb s3://nexpay-receipts
awslocal s3 mb s3://nexpay-reports

# DynamoDB Tables

echo "Creating DynamoDB tables..."

awslocal dynamodb create-table \
    --table-name transaction-history \
    --attribute-definitions \
        AttributeName=userId,AttributeType=S \
        AttributeName=transactionId,AttributeType=S \
        AttributeName=createdAt,AttributeType=S \
    --key-schema \
        AttributeName=userId,KeyType=HASH \
        AttributeName=transactionId,KeyType=RANGE \
    --global-secondary-indexes '[
        {
            "IndexName": "CreatedAtIndex",
            "KeySchema": [
                {"AttributeName": "userId", "KeyType": "HASH"},
                {"AttributeName": "createdAt", "KeyType": "RANGE"}
            ],
            "Projection": {"ProjectionType": "ALL"},
            "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
        }
    ]' \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

# Secrets Manager

echo "Creating secrets..."

awslocal secretsmanager create-secret \
    --name nexpay/database \
    --secret-string '{"username":"nexpay","password":"nexpay_secret","host":"postgres","port":5432}'

awslocal secretsmanager create-secret \
    --name nexpay/jwt \
    --secret-string '{"secret":"nexpay-jwt-super-secret-key-for-development-only-change-in-prod","expiration":3600}'

# Summary

echo ""
echo "AWS resources initialized:"
echo "  SQS:      payment-events, payment-events-dlq"
echo "  SNS:      payment-notifications, user-alerts"
echo "  S3:       nexpay-receipts, nexpay-reports"
echo "  DynamoDB: transaction-history"
echo "  Secrets:  nexpay/database, nexpay/jwt"
echo ""
