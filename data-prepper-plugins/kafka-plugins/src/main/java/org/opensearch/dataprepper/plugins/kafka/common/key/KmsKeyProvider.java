package org.opensearch.dataprepper.plugins.kafka.common.key;

import org.opensearch.dataprepper.plugins.kafka.common.aws.AwsContext;
import org.opensearch.dataprepper.plugins.kafka.configuration.TopicConfig;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptResponse;

import java.util.Base64;

class KmsKeyProvider implements InnerKeyProvider {
    private final AwsContext awsContext;

    public KmsKeyProvider(AwsContext awsContext) {
        this.awsContext = awsContext;
    }

    @Override
    public boolean supportsConfiguration(TopicConfig topicConfig) {
        return topicConfig.getKmsKeyId() != null;
    }

    @Override
    public byte[] apply(TopicConfig topicConfig) {
        String kmsKeyId = topicConfig.getKmsKeyId();

        AwsCredentialsProvider awsCredentialsProvider = awsContext.get();

        KmsClient kmsClient = KmsClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(awsContext.getRegion())
                .build();

        byte[] decodedEncryptionKey = Base64.getDecoder().decode(topicConfig.getEncryptionKey());
        DecryptResponse decryptResponse = kmsClient.decrypt(builder -> builder
                .keyId(kmsKeyId)
                .ciphertextBlob(SdkBytes.fromByteArray(decodedEncryptionKey))
        );

        return decryptResponse.plaintext().asByteArray();
    }
}
