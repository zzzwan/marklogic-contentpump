/*
 * Copyright 2003-2012 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.contentpump;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.ReflectionUtils;

public class SequenceFileReader<VALUEIN> extends ImportRecordReader<VALUEIN> {
    public static final Log LOG = LogFactory.getLog(SequenceFileReader.class);
    protected SequenceFile.Reader reader;
    protected Writable seqKey;
    protected Writable seqValue;
    protected boolean hasNext = true;

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    @Override
    public float getProgress() throws IOException, InterruptedException {
        return hasNext == true ? 0 : 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(InputSplit inSplit, TaskAttemptContext context)
        throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        Path file = ((FileSplit) inSplit).getPath();

        prefix = conf.get(ConfigConstants.CONF_OUTPUT_URI_PREFIX);
        suffix = conf.get(ConfigConstants.CONF_OUTPUT_URI_SUFFIX);

        FileSystem fs = file.getFileSystem(context.getConfiguration());
        reader = new SequenceFile.Reader(fs, file, conf);
        String keyClass = conf
            .get(ConfigConstants.CONF_INPUT_SEQUENCEFILE_KEY_CLASS);
        String valueClass = conf
            .get(ConfigConstants.CONF_INPUT_SEQUENCEFILE_VALUE_CLASS);
        String valueType = conf
            .get(ConfigConstants.CONF_INPUT_SEQUENCEFILE_VALUE_TYPE);
        SequenceFileValueType svType = SequenceFileValueType
            .valueOf(valueType);
        Class<? extends Writable> vClass = svType.getWritableClass();
        value = (VALUEIN) ReflectionUtils.newInstance(vClass, conf);

        if (!reader.getKeyClass().getCanonicalName().equals(keyClass)) {
            throw new IOException("Key class of sequence file on HDFS is "
                + keyClass
                + "which is inconsistent with the one in configuration "
                + reader.getKeyClass().getCanonicalName());
        }
        if (!reader.getValueClass().getCanonicalName().equals(valueClass)) {
            throw new IOException("Value class of sequence file on HDFS is "
                + valueClass
                + "which is inconsistent with the one in configuration "
                + reader.getValueClass().getCanonicalName());
        }
        seqKey = (Writable) ReflectionUtils.newInstance(reader.getKeyClass(),
            conf);
        seqValue = (Writable) ReflectionUtils.newInstance(
            reader.getValueClass(), conf);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        if (reader == null) {
            return false;
        }

        while (reader.next(seqKey, seqValue)) {
            setKey(((SequenceFileKey) seqKey).getDocumentURI().getUri());

            if (value instanceof Text) {
                ((Text) value).set(((SequenceFileValue<Text>) seqValue)
                    .getValue());
            } else if (value instanceof BytesWritable) {
                ((BytesWritable) value)
                    .set(((SequenceFileValue<BytesWritable>) seqValue)
                        .getValue());
            } else {
                LOG.error("Unexpected type: " + value.getClass());
                key = null;
            }
            return true;
        }
        return false;
    }
}
