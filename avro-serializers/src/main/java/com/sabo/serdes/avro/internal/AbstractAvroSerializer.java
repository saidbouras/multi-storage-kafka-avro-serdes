/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sabo.serdes.avro.internal;

import com.sabo.serdes.avro.config.KafkaAvroSerDeConfig;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.serializers.NonRecordContainer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.errors.SerializationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class AbstractAvroSerializer extends AbstractAvroSerDe {

    private final EncoderFactory encoderFactory = EncoderFactory.get();
    protected boolean autoRegisterSchema;

    protected void configure(KafkaAvroSerDeConfig config) {
        autoRegisterSchema = config.autoRegisterSchema();
        schemaClient = config.getSchemaClient();
        schemaClient.configureClientProperties(config.getSerConfig());
    }

    protected KafkaAvroSerializerConfig serializerConfig(Map<String, ?> props) {
        try {
            return new KafkaAvroSerializerConfig(props);
        } catch (io.confluent.common.config.ConfigException e) {
            throw new ConfigException(e.getMessage());
        }
    }

    protected byte[] serializeImpl(String subject, Object object) throws SerializationException {
        Schema schema = null;
        // null needs to treated specially since the client most likely just wants to send
        // an individual null value instead of making the subject a null type. Also, null in
        // Kafka has a special meaning for deletion in a topic with the compact retention policy.
        // Therefore, we will bypass schema registration and return a null value in Kafka, instead
        // of an Avro encoded null.
        if (object == null) {
            return null;
        }
        String restClientErrorMsg = "";
        try {
            int id;
            schema = getSchema(object);
            if (autoRegisterSchema) {
                restClientErrorMsg = "Error registering Avro schema: ";
                id = schemaClient.register(subject, schema);
            } else {
                restClientErrorMsg = "Error retrieving Avro schema: ";
                id = schemaClient.getId(subject, schema);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(MAGIC_BYTE);
            out.write(ByteBuffer.allocate(idSize).putInt(id).array());
            if (object instanceof byte[]) {
                out.write((byte[]) object);
            } else {
                BinaryEncoder encoder = encoderFactory.directBinaryEncoder(out, null);
                DatumWriter<Object> writer;
                Object
                    value =
                    object instanceof NonRecordContainer ? ((NonRecordContainer) object).getValue()
                        : object;
                if (value instanceof SpecificRecord) {
                    writer = new SpecificDatumWriter<>(schema);
                } else {
                    writer = new GenericDatumWriter<>(schema);
                }
                writer.write(value, encoder);
                encoder.flush();
            }
            byte[] bytes = out.toByteArray();
            out.close();
            return bytes;
        } catch (IOException | RuntimeException e) {
            // avro serialization can throw AvroRuntimeException, NullPointerException,
            // ClassCastException, etc
            throw new SerializationException("Error serializing Avro message", e);
        } catch (RestClientException e) {
            throw new SerializationException(restClientErrorMsg + schema, e);
        }
    }
}