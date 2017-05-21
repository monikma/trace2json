package com.example.trace2json;

import com.example.trace2json.process.CallsProcessor;
import com.example.trace2json.process.impl.DefaultCallsProcessor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.google.common.io.Resources;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;
import java.util.stream.Collectors;


public class Trace2JsonProcessor
{
	private static final String[] ALLOWED_DATE_FORMATS = new String[]{
			"yyyy-MM-dd'T'HH:mm:ss.SS'Z'",
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
			"yyyy-MM-dd'T'HH:mm:ss'Z'"};

	private Collection<DateTimeFormatter> dateTimeFormatters;
	private ObjectMapper jsonMapper;
	private Writer outputWriter;
	private CallsProcessor logsProcessor = new DefaultCallsProcessor();
	private Scanner scanner;

	public static void main(final String... args) throws IOException
	{
		final URL url = Resources.getResource("medium-log.txt");
		Trace2JsonProcessor proc = new Trace2JsonProcessor(
				new FileReader(new File(url.getFile())),
				new FileWriter(new File("src/main/resources/output.txt")));
		proc.process();
	}

	public Trace2JsonProcessor(final Reader inputReader, final Writer outputWriter)
	{

		this.jsonMapper = new ObjectMapper();
		this.jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);


		this.outputWriter = outputWriter;
		this.scanner = new Scanner(inputReader);

		this.dateTimeFormatters = Arrays
				.stream(ALLOWED_DATE_FORMATS)
				.map(df -> DateTimeFormatter.ofPattern(df))
				.collect(Collectors.toList());
	}

	private void process()
	{
		SequenceWriter writer = null;
		try
		{
			writer = jsonMapper
					.writer(new MinimalPrettyPrinter(System.getProperty("line.separator")))
					.writeValues(outputWriter);

			while (scanner.hasNext())
			{
				final Call call = readCallLine(scanner);
				logsProcessor.processCall(call);
				writer.writeAll(logsProcessor.popReadyTraces(false));
			}

			writer.writeAll(logsProcessor.popReadyTraces(true));
			writer.close();
		}
		catch (final IOException e)
		{
			System.err.println("Problem writing or reading file: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			scanner.close();
		}
	}

	private Call readCallLine(final Scanner scanner)
	{
		final LocalDateTime start = parseDateTime(scanner.next());
		final LocalDateTime end = parseDateTime(scanner.next());
		final String traceId = scanner.next();
		final String service = scanner.next();
		final String[] spans = scanner.next().split("->");
		final String callerSpanId = "null".equals(spans[0]) ? null : spans[0];
		final String spanId = spans[1];
		return new Call(start, end, traceId, service, callerSpanId, spanId);
	}

	private LocalDateTime parseDateTime(final String str)
	{
		for (DateTimeFormatter dtf : dateTimeFormatters)
		{
			try
			{
				return LocalDateTime.parse(str, dtf);
			}
			catch (DateTimeParseException e)
			{
				// keep trying
			}
		}
		throw new IllegalArgumentException("Unknown date format: " + str);
	}

}
