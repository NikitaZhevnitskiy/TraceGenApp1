package com.zhenik.opentracing.jegos.sample;

import io.opentracing.ActiveSpan;
import io.opentracing.References;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class OpenTracingTest {

  private static void waitABit() {
    try {
      Thread.sleep((long) (Math.random() * 1000));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws InterruptedException {
    String processId = "1";

    final Tracer goHome =
        TracerFactory.getInstance(TracerFactory.Provider.JAEGER, "Main Process");
    final MainProcess mainProcess = new MainProcess(goHome);
    Map<String, String> context = mainProcess.mainTask(processId);

    final Tracer buyFood =
        TracerFactory.getInstance(TracerFactory.Provider.JAEGER, "Sub Process");
    final OtherProcess otherProcess = new OtherProcess(buyFood);
    otherProcess.anotherTask(processId, context);

    //Shutdown process
    Thread.sleep(2000);
  }
  ////////////

  ////////////////////
  // Node 1 Process //
  ////////////////////
  static class MainProcess {
    private final Tracer tracer;

    MainProcess(Tracer tracer) {
      this.tracer = tracer;
    }

    Map<String, String> mainTask(String processId) {
      ActiveSpan span =
          tracer.buildSpan("goingHomeSpan")
              .withTag("processId", processId)
              .startActive();

      waitABit();

      goHome(span);

      span.close();


      Map<String, String> context = new HashMap<>();
      tracer.inject(
          span.context(),
          Format.Builtin.TEXT_MAP,
          new TextMapInjectAdapter(context));
      return context;
    }

    private void goHome(ActiveSpan parentSpan) {
      ActiveSpan span =
          tracer.buildSpan("goHomeSpan").startActive();

      waitABit();

      span.log("goHomeLog");

      span.close();
    }
  }

  ////////////////////
  // Node 2 Process //
  ////////////////////
  static class OtherProcess {
    private final Tracer tracer;

    OtherProcess(Tracer tracer) {
      this.tracer = tracer;
    }

    void anotherTask(String processId,Map<String, String> context) {
      SpanContext spanContext = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(context));

      ActiveSpan span =
          tracer.buildSpan("buyingFoodSpan")
              .withTag("processId", processId)
              .addReference(References.FOLLOWS_FROM, spanContext)
              .startActive();
      waitABit();
      span.close();
    }
  }
}
