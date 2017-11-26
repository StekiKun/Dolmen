package common;

import java.io.IOException;
import java.io.Writer;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Implementation of {@link Writer} which delegates all operations
 * to an underlying instance of {@link Writer}, but counts the
 * characters that are written to the stream along the way.
 * The current count of characters can be retrieved with {@link #getCount()}.
 * 
 * @author Stéphane Lescuyer
 */
public class CountingWriter extends Writer {
	
	/**
	 * The underlying writer instance to which all writes
	 * are delegated
	 */
	private final Writer out;
	
	/**
	 * Counts the number of <b>characters</b> which have been
	 * written (which does not mean that they have been flushed)
	 */
	private long count;
	
	/**
	 * Creates a writer wrapping {@code out} which counts the
	 * number of characters written into it. It will of course
	 * not take into account the characters which have already
	 * been written to {@code out}, or those which may be written
	 * to {@code out} directly in the future (i.e. by keeping a
	 * reference to {@code out} and using it outside of this class,
	 * which of course is really not recommended).
	 * 
	 * @param out
	 */
	public CountingWriter(Writer out) {
		this.out = out;
		this.count = 0;
	}
	
	/**
	 * @return the number of characters written into this
	 * 	instance since its creation
	 */
	public long getCount() {
		return count;
	}

	@Override
	public void write(int c) throws IOException {
		out.write(c);
		++count;
	}

	@Override
	public void write(char @Nullable[] cbuf) throws IOException {
		if (cbuf == null)
			throw new IllegalArgumentException("write(null) is not allowed");
		out.write(cbuf);
		count += cbuf.length;
	}

	@Override
	public void write(char @Nullable[] cbuf, int off, int len) throws IOException {
		if (cbuf == null)
			throw new IllegalArgumentException("write(null, off, len) is not allowed");
		out.write(cbuf, off, len);
		count += len;
	}

	@Override
	public void write(@Nullable String str) throws IOException {
		if (str == null)
			throw new IllegalArgumentException("write(null) is not allowed");
		out.write(str);
		count += str.length();
	}

	@Override
	public void write(@Nullable String str, int off, int len) throws IOException {
		if (str == null)
			throw new IllegalArgumentException("write(null, off, len) is not allowed");
		out.write(str, off, len);
		count += len;
	}

	@Override
	public CountingWriter append(@Nullable CharSequence csq) throws IOException {
		out.append(csq);
		count += csq == null ? 4 : csq.length();
		return this;
	}

	@Override
	public CountingWriter append(@Nullable CharSequence csq, int start, int end) throws IOException {
		out.append(csq, start, end);
		count += (end - start);
		return this;
	}

	@Override
	public CountingWriter append(char c) throws IOException {
		out.append(c);
		++count;
		return this;
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}
	
}
