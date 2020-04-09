package github.lsa5521.k8scontroller;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.JaegerSpanContext;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.openapi.models.V1Status;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
//import lesson05.solution.Hello;
import lib.Tracing;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;

public class K8sController {
    
    private final Tracer tracer;
    private ApiClient client;
    private CoreV1Api api;

    private K8sController(Tracer tracer, ApiClient client, CoreV1Api api ) {
        this.tracer = tracer;
        this.client = client;
        this.api = api;
    }
    
    private void startFormaterPod(String helloto, String parentcontext, String diagnosticID)
    {
        try {
        V1Pod pod =
                new V1PodBuilder()
                        .withNewMetadata()
                        .withName("ot-demo-formater")
                        .endMetadata()
                        .withNewSpec()
                        .addNewContainer()
                        //.addToCommand("java","-DJAEGER_AGENT_HOST=192.168.1.113", "-cp", "./otdockerexample.jar:./dependency/*","lesson05.solution.Formatter", "bob", "99f9848e8fd25a9e:99f9848e8fd25a9e:0:1")
                        .addToCommand("java","-DJAEGER_AGENT_HOST=192.168.1.113", "-cp", "./otdockerexample.jar:./dependency/*","lesson05.solution.Formatter", helloto, parentcontext,diagnosticID)
                        .withImage("otexample_image:latest")
                        .withImagePullPolicy("IfNotPresent")
                        .withName("ot-demo-formater")
                        .endContainer()
                        .withRestartPolicy("Never")
                        .endSpec()
                        .build();

        this.api.createNamespacedPod("default", pod, null, null, null);
        }
        //catch(io.kubernetes.client.openapi.ApiException e)
        catch(Exception e)
        {
        	System.out.println("Exception during POD creation " + e.getMessage());
        }
    }
    
    private void startPublisherPod(String hellstr, String parentcontext, String diagnosticID)
    {
        try {
        V1Pod pod =
                new V1PodBuilder()
                        .withNewMetadata()
                        .withName("ot-demo-publisher")
                        .endMetadata()
                        .withNewSpec()
                        .addNewContainer()
                        //.addToCommand("java","-DJAEGER_AGENT_HOST=192.168.1.113", "-cp", "./otdockerexample.jar:./dependency/*","lesson05.solution.Publisher", "bob", "99f9848e8fd25a9e:99f9848e8fd25a9e:0:1")
                        .addToCommand("java","-DJAEGER_AGENT_HOST=192.168.1.113", "-cp", "./otdockerexample.jar:./dependency/*","lesson05.solution.Publisher", hellstr, parentcontext, diagnosticID)
                        .withImage("otexample_image:latest")
                        .withImagePullPolicy("IfNotPresent")
                        .withName("ot-demo-publisher")
                        .endContainer()
                        .withRestartPolicy("Never")
                        .endSpec()
                        .build();

        this.api.createNamespacedPod("default", pod, null, null, null);
        }
        //catch(io.kubernetes.client.openapi.ApiException e)
        catch(Exception e)
        {
        	System.out.println("Exception during creation of POD "+ e.getMessage());
        }
    }
    
    private void printLog(String namespace, String podName) throws ApiException {
    	  // https://github.com/kubernetes-client/java/blob/master/kubernetes/docs/CoreV1Api.md#readNamespacedPodLog
    	  String readNamespacedPodLog =
    	    this.api.readNamespacedPodLog(
    	      podName,
    	      namespace,
    	      null,
    	      Boolean.FALSE,
    	      Integer.MAX_VALUE,
    	      null,
    	      Boolean.FALSE,
    	      Integer.MAX_VALUE,
    	      40,
    	      Boolean.FALSE);
    	  System.out.println(readNamespacedPodLog);
    	 }

    
    private void sayHello(String helloTo, String greeting,String diagnosticID) {
        try (Scope scope = tracer.buildSpan("say-hello").startActive(true)) {
            scope.span().setTag("diagnosticID", diagnosticID);
            scope.span().setBaggageItem("greeting", greeting);
            //parent span from here in text format needs to be sent to format and publish components
            String helloStr = formatString(helloTo,diagnosticID);
            printHello(helloStr,diagnosticID);
        }
    }

    private String formatString(String helloTo, String diagnosticID) {
        try (Scope scope = tracer.buildSpan("formatString").startActive(true)) {
            //String helloStr = getHttp(8081, "format", "helloTo", helloTo);
        	//String helloStr = "";          	//need to call format process

            scope.span().log(ImmutableMap.of("event", "string-format", "value", helloTo));
        	SpanContext childSpanCtx = scope.span().context();
        	String childSpanCtxStr = Tracing.contextAsString((JaegerSpanContext)childSpanCtx);
        	this.startFormaterPod(helloTo,childSpanCtxStr,diagnosticID);
        	//print log from the pod
        	try {
        		Thread.sleep(5000);
        		this.printLog("default","ot-demo-formater");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return helloTo;
        }
    }

    private void printHello(String helloStr, String diagnosticID) {
        try (Scope scope = tracer.buildSpan("printHello").startActive(true)) {
            //getHttp(8082, "publish", "helloStr", helloStr);
        	//need to call publish process

            scope.span().log(ImmutableMap.of("event", "println"));
        	SpanContext childSpanCtx = scope.span().context();
        	String childSpanCtxStr = Tracing.contextAsString((JaegerSpanContext)childSpanCtx);
        	this.startPublisherPod(helloStr,childSpanCtxStr,diagnosticID);

        	//print log from the pod
        	try {
                Thread.sleep(5000);
				this.printLog("default","ot-demo-publisher");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
	
    public static void main(String[] args) throws IOException, ApiException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();

        //delete PODs previously created

        try
        {	
	        V1Status deleteResult = api.deleteNamespacedPod("ot-demo-formater", "default", null, null, 0, false, null, null);
	        Thread.sleep(5000);
	        System.out.println(deleteResult);
	        
        }
        catch (Exception e)
        {
        	System.out.println(" PODs do not exist");
        	System.out.println(e);
        }

        try
        {
	        V1Status deleteResult1 = api.deleteNamespacedPod("ot-demo-publisher", "default", null, null, 0, false, null, null);
	        Thread.sleep(5000);
	        System.out.println(deleteResult1);

        }
        //catch (io.kubernetes.client.openapi.ApiException e)
        catch (Exception e)
        {
        	System.out.println(" PODs do not exist");
        	System.out.println(e);
        }
        

        
        if (args.length != 3) {
            throw new IllegalArgumentException("Expecting three arguments, helloTo, greeting and diagnosticid");
        }
        String helloTo = args[0];
        String greeting = args[1];
        String diagnosticID = args[2];
        try (JaegerTracer tracer = Tracing.init("hello-world")) {
            new K8sController(tracer,client,api).sayHello(helloTo, greeting,diagnosticID);
        }
    }
}