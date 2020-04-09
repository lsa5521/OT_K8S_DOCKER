package lib;

//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;

//import javax.ws.rs.core.MultivaluedMap;

//import com.google.common.collect.ImmutableMap;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
//import io.opentracing.propagation.Format;
//import io.opentracing.propagation.TextMap;
//import io.opentracing.propagation.TextMapExtractAdapter;
//import io.opentracing.tag.Tags;
//import okhttp3.Request;

//for codec

import io.jaegertracing.internal.exceptions.EmptyTracerStateStringException;
import io.jaegertracing.internal.exceptions.MalformedTracerStateStringException;
import io.jaegertracing.internal.exceptions.TraceIdOutOfBoundException;
import io.jaegertracing.internal.JaegerSpanContext;
import java.math.BigInteger;
//import io.jaegertracing.internal.JaegerObjectFactory;

public final class Tracing {
    private Tracing() {
    }

    public static JaegerTracer init(String service) {
        SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv()
                .withType(ConstSampler.TYPE)
                .withParam(1);

        ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv()
                .withLogSpans(true);

        Configuration config = new Configuration(service)
                .withSampler(samplerConfig)
                .withReporter(reporterConfig);

        return config.getTracer();
    }

//    public static Scope startServerSpan(Tracer tracer, javax.ws.rs.core.HttpHeaders httpHeaders, String operationName) {
//        // format the headers for extraction
//        MultivaluedMap<String, String> rawHeaders = httpHeaders.getRequestHeaders();
//        final HashMap<String, String> headers = new HashMap<String, String>();
//        for (String key : rawHeaders.keySet()) {
//            headers.put(key, rawHeaders.get(key).get(0));
//        }
//
//        Tracer.SpanBuilder spanBuilder;
//        try {
//            SpanContext parentSpanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
//            if (parentSpanCtx == null) {
//                spanBuilder = tracer.buildSpan(operationName);
//            } else {
//                spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanCtx);
//            }
//        } catch (IllegalArgumentException e) {
//            spanBuilder = tracer.buildSpan(operationName);
//        }
//        // TODO could add more tags like http.url
//        return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(true);
//    }

    public static Scope startComponentSpan(Tracer tracer, SpanContext parentSpanCtx, String operationName) {
        // format the headers for extraction
//        MultivaluedMap<String, String> rawHeaders = httpHeaders.getRequestHeaders();
//        final HashMap<String, String> headers = new HashMap<String, String>();
//        for (String key : rawHeaders.keySet()) {
//            headers.put(key, rawHeaders.get(key).get(0));
//        }

        Tracer.SpanBuilder spanBuilder;
        try {
            //SpanContext parentSpanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
            if (parentSpanCtx == null) {
                spanBuilder = tracer.buildSpan(operationName);
            } else {
                spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanCtx);
            }
        } catch (IllegalArgumentException e) {
            spanBuilder = tracer.buildSpan(operationName);
        }
        // TODO could add more tags like http.url
    
        Scope scope = null;
        //try (Scope scope = spanBuilder.startActive(true))
        try 
           {
        	    scope = spanBuilder.startActive(true);
        		System.out.println("span should be started here");
        	} 
        catch (Exception e)
        	{
        		System.out.println("Exception");
        	};
        return  scope;
        
        //return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(true);
    }
    
//    public static TextMap requestBuilderCarrier(final Request.Builder builder) {
//        return new TextMap() {
//            @Override
//            public Iterator<Map.Entry<String, String>> iterator() {
//                throw new UnsupportedOperationException("carrier is write-only");
//            }
//
//            @Override
//            public void put(String key, String value) {
//                builder.addHeader(key, value);
//            }
//        };
//    }
    //from TextMapCodec 
    
    /**
     * Parses a full (low + high) traceId, trimming the lower 64 bits.
     * @param hexString a full traceId
     * @return the long value of the higher 64 bits for a 128 bit traceId or 0 for 64 bit traceIds
     */
    private static long high(String hexString) {
      if (hexString.length() > 16) {
        int highLength = hexString.length() - 16;
        String highString = hexString.substring(0, highLength);
        return new BigInteger(highString, 16).longValue();
      }
      return 0L;
    }
    
    public static JaegerSpanContext contextFromString(String value)
    	      throws MalformedTracerStateStringException, EmptyTracerStateStringException {
    	    if (value == null || value.equals("")) {
    	      throw new EmptyTracerStateStringException();
    	    }

    	    String[] parts = value.split(":");
    	    if (parts.length != 4) {
    	      throw new MalformedTracerStateStringException(value);
    	    }

    	    String traceId = parts[0];
    	    if (traceId.length() > 32 || traceId.length() < 1) {
    	      throw new TraceIdOutOfBoundException("Trace id [" + traceId + "] length is not withing 1 and 32");
    	    }

    	    // TODO(isaachier): When we drop Java 1.6 support, use Long.parseUnsignedLong instead of using BigInteger.
    	    return new JaegerSpanContext(
    	        high(traceId),
    	        new BigInteger(traceId, 16).longValue(),
    	        new BigInteger(parts[1], 16).longValue(),
    	        new BigInteger(parts[2], 16).longValue(),
    	        new BigInteger(parts[3], 16).byteValue());
    	  }
    /**
     * Encode context into a string.
     * @param context Span context to encode.
     * @return Encoded string representing span context.
     */
    public static String contextAsString(JaegerSpanContext context) {
      int intFlag = context.getFlags() & 0xFF;
      return new StringBuilder()
          .append(context.getTraceId()).append(":")
          .append(Long.toHexString(context.getSpanId())).append(":")
          .append(Long.toHexString(context.getParentId())).append(":")
          .append(Integer.toHexString(intFlag))
          .toString();
    }
}
