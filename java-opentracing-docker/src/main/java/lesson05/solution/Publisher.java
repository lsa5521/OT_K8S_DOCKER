package lesson05.solution;

import java.util.UUID;

import com.google.common.collect.ImmutableMap;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;
import io.opentracing.Tracer;

import io.opentracing.SpanContext;
import io.jaegertracing.internal.JaegerSpanContext;

import lib.Tracing;


public class Publisher {


	private final Tracer tracer;
    private final String parentSpanCtxString;
    private final String tag;
	
    private Publisher(Tracer tracer, String parentSpanCtxString, String tag) {
	    this.tracer = tracer;
	    this.parentSpanCtxString = parentSpanCtxString;
	    this.tag = tag;
    }
    
    public  String formatString(String helloTo) {
        SpanContext parentSpanCtx = Tracing.contextFromString(this.parentSpanCtxString);                       
        try (Scope scope = Tracing.startComponentSpan(tracer, parentSpanCtx, "publish")) {	  
      	  //System.out.println(hello);
          String helloStr = String.format("Hello, %s!", helloTo);	
      	  scope.span().log(ImmutableMap.of("event", "println", "value", helloStr));
      	  SpanContext childSpanCtx = scope.span().context();
      	  scope.span().setTag("diagnosticid", this.tag);
      	  return "published";
        }        
        
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("Expecting three argument");
        }

        String helloTo = args[0];
        String parentSpanCtxString = args[1];
        String tag = args[2];
    	
    	Publisher publisher = new Publisher(Tracing.init("publisher"), parentSpanCtxString, tag);
    	String published = publisher.formatString(helloTo);
    	System.out.println(published);
    }
}
      

