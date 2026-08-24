package chapter6.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

	private static final DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

	private static final Map<Level, String> levelMsgMap = Collections.unmodifiableMap(
			new HashMap<Level, String>() {
				{
					put(Level.SEVERE, "ERROR");
					put(Level.WARNING, "WARN");
					put(Level.INFO, "INFO");
					put(Level.CONFIG, "CONF");
					put(Level.FINE, "FINE");
					put(Level.FINER, "FINE");
					put(Level.FINEST, "FINE");
				}
			});

	@Override
	public String format(LogRecord record) {

		StringBuilder sb = new StringBuilder(200);

		Date instant = new Date(record.getMillis());
		sb.append(formatter.format(instant));
		sb.append(" ");

		sb.append(levelMsgMap.get(record.getLevel()));
		sb.append(" ");

		sb.append(formatMessage(record));

		sb.append(System.lineSeparator());
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
			}
		}

		return sb.toString();
	}

}
