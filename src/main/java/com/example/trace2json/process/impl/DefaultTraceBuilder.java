package com.example.trace2json.process.impl;


import com.example.trace2json.Call;
import com.example.trace2json.pojo.Trace;
import com.example.trace2json.pojo.TraceRoot;
import com.example.trace2json.process.TraceBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class DefaultTraceBuilder implements TraceBuilder
{
	private TraceRoot rootTrace;
	private Map<String, Trace> spanToTrace;
	private LocalDateTime endTime;

	/**
	 * Creates a new instance of the processor for given trace ID.
	 *
	 * @param traceId the trace ID for this trace.
	 */
	public DefaultTraceBuilder(final String traceId)
	{
		this.rootTrace = new TraceRoot();
		this.rootTrace.setId(traceId);
		this.spanToTrace = new HashMap<>();
	}

	@Override
	public void processCall(final Call call)
	{
		final Trace trace = new Trace();
		trace.setEnd(call.getEndTime());
		trace.setService(call.getService());
		trace.setStart(call.getStartTime());
		trace.setSpan(call.getSpanId());
		trace.setCallerSpanId(call.getCallerSpanId());
		trace.setOrphaned(true);
		trace.setCalls(new ArrayList<>());

		if (call.getCallerSpanId() == null)
		{
			this.endTime = call.getEndTime();
			this.rootTrace.setRoot(trace);
			trace.setOrphaned(false);
		}
		spanToTrace.put(trace.getSpan(), trace);
	}

	@Override
	public TraceRoot buildTrace()
	{
		for (Trace trace : spanToTrace.values())
		{
			if (trace.getCallerSpanId() == null)
			{
				continue;
			}
			Trace predecessor = spanToTrace.get(trace.getCallerSpanId());
			if (predecessor != null)
			{
				predecessor.getCalls().add(trace);
				trace.setOrphaned(false);
			}
		}
		if (spanToTrace.values().stream().filter(t -> t.isOrphaned()).findAny().isPresent())
		{
			throw new IllegalArgumentException("The trace '" + rootTrace.getId() + "' is not finished.");
		}
		return rootTrace;
	}

	@Override
	public LocalDateTime getEndTimeOrNull()
	{
		return endTime;
	}

	@Override
	public String toString()
	{
		return rootTrace.getId();
	}
}
