package org.plumelib.bibtex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.plumelib.util.EntryReader;

/** Tests for {@link BibtexClean}. */
public final class BibtexCleanTest {

  /**
   * Runs {@link BibtexClean#clean} on the given input and returns the cleaned output.
   *
   * @param input the BibTeX text to clean
   * @return the cleaned BibTeX text
   * @throws IOException if there is a problem reading the input
   */
  private static String cleaned(String input) throws IOException {
    StringWriter sw = new StringWriter();
    try (EntryReader er = new EntryReader(new StringReader(input));
        PrintWriter pw = new PrintWriter(sw)) {
      BibtexClean.clean(er, pw);
    }
    return sw.toString();
  }

  /**
   * Joins the given lines, terminating each with the platform line separator (which is what {@link
   * PrintWriter#println} emits).
   *
   * @param lines the lines to join
   * @return the joined lines, each terminated by a line separator
   */
  private static String lines(String... lines) {
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      sb.append(line).append(System.lineSeparator());
    }
    return sb.toString();
  }

  @Test
  public void removesTextOutsideEntries() throws IOException {
    String input =
        lines(
            "Junk before the entry.",
            "@article{key,",
            "  author = {Smith},",
            "  title = {A Title},",
            "  year = 2020",
            "}",
            "Junk after the entry.");
    String expected =
        lines("@article{key,", "  author = {Smith},", "  title = {A Title},", "  year = 2020", "}");
    assertEquals(expected, cleaned(input));
  }

  @Test
  public void keepsCommentLines() throws IOException {
    String input = lines("% keep this comment", "drop this text");
    String expected = lines("% keep this comment");
    assertEquals(expected, cleaned(input));
  }

  @Test
  public void keepsBlankLinesBetweenEntries() throws IOException {
    String input = lines("@string{pub = \"Publisher\"}", "", "@string{jrnl = \"Journal\"}");
    assertEquals(input, cleaned(input));
  }

  @Test
  public void keepsSingleLineStringDefinition() throws IOException {
    String input = lines("noise", "@string{pub = \"Publisher\"}", "more noise");
    String expected = lines("@string{pub = \"Publisher\"}");
    assertEquals(expected, cleaned(input));
  }

  @Test
  public void handlesQuotedFieldValueAndParenDelimiters() throws IOException {
    String input = lines("@article(key,", "  title = \"A quoted title\",", "  month = Jan", ")");
    assertEquals(input, cleaned(input));
  }

  @Test
  public void closesEntryOnClosingBraceLine() throws IOException {
    String input = lines("@book{k,", "  title = {T}", "}", "trailing junk that must be dropped");
    String expected = lines("@book{k,", "  title = {T}", "}");
    assertEquals(expected, cleaned(input));
  }

  @Test
  public void unterminatedEntryAtEofDoesNotCrash() throws IOException {
    // An entry that is never closed before end of input should be copied out verbatim (with a
    // diagnostic written to standard error), not throw an exception.
    String input = lines("@book{k,", "  title = {T}");
    assertEquals(input, cleaned(input));
  }
}
