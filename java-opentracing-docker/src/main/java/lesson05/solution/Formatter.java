package lesson05.solution;

import com.google.common.collect.ImmutableMap;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

import lib.Tracing;

public class Formatter {

    private final Tracer tracer;
    private final String parentSpanCtxString;
    private final String tag;

    private Formatter(Tracer tracer, String parentSpanCtxString, String tag) {
        this.tracer = tracer;
        this.parentSpanCtxString = parentSpanCtxString;
        this.tag = tag;
    }
    
    public  String formatString(String helloTo) {
        SpanContext parentSpanCtx2 = Tracing.contextFromString(this.parentSpanCtxString);
        
        try (Scope scope = Tracing.startComponentSpan(tracer, parentSpanCtx2, "format")) {	  
      	  //System.out.println(hello);
          String helloStr = String.format("Hello, %s!", helloTo);	
      	  scope.span().log(ImmutableMap.of("event", "format", "value", helloStr));
      	  SpanContext childSpanCtx = scope.span().context();
      	  scope.span().setTag("diagnosticid", this.tag);
      	  return helloStr;
        }        
        
    }


    public static void main(String[] args) throws Exception {

    	
        if (args.length != 3) {
            throw new IllegalArgumentException("Expecting three argument");
        }

        String helloTo = args[0];
        String parentSpanCtxString = args[1];
        String tag = args[2];
    	
    	Formatter formatter = new Formatter(Tracing.init("formatter"), parentSpanCtxString,tag);
    	String formatted = formatter.formatString(helloTo);
    	System.out.println(formatted);
    	
    }
}
