package com.marklogic.mapreduce;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DefaultStringifier;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;

/**
 * MarkLogicOutputFormat for Document Property.
 * 
 * <p>
 *  Use this class to store MapReduce results as properties on documents
 *  in a MarkLogic database. This class expects output key-value pairs
 *  where the key is a {@link DocumentURI} and the value is a {@link MarkLogicNode}
 *  describing the property to be added to the document at the key URI.
 * </p>
 * <p>
 *  Control whether the inserted property replaces or adds to existing
 *  document properties by setting the configuration property
 *  {@link MarkLogicConstants#DEFAULT_PROPERTY_OPERATION_TYPE output.property.optype}.
 *  By default, any existing properties are replaced with the new one.
 * </p>
 * <p>
 *  By default, properties are only created by documents that exist in the
 *  database. Set the configuration property
 *  {@link MarkLogicConstants#OUTPUT_PROPERTY_ALWAYS_CREATE output.property.alwayscreate}
 *  to true to create properties even if the target document does not exist.
 * </p>
 * 
 * @see PropertyOpType
 * 
 * @author jchen
 */
public class PropertyOutputFormat 
extends MarkLogicOutputFormat<DocumentURI, MarkLogicNode> {
    public static final Log LOG =
        LogFactory.getLog(PropertyOutputFormat.class);
    
    @Override
    public RecordWriter<DocumentURI, MarkLogicNode> getRecordWriter(
            TaskAttemptContext context) throws IOException, InterruptedException {        
        Configuration conf = context.getConfiguration();
        LinkedMapWritable forestHostMap = 
            DefaultStringifier.load(conf, OUTPUT_FOREST_HOST, 
                    LinkedMapWritable.class);
        
        try {
            int taskId = context.getTaskAttemptID().getTaskID().getId();
            String host = InternalUtilities.getHost(taskId, forestHostMap);
            URI serverUri = InternalUtilities.getOutputServerUri(conf, host);
            return new PropertyWriter(serverUri, conf);
        } catch (URISyntaxException e) {
            LOG.error(e);
            throw new IOException(e);
        }
    }

    @Override
    void checkOutputSpecs(Configuration conf, Session session,
            AdhocQuery query, ResultSequence result) throws RequestException {
        // No extra check needed       
    }

}
