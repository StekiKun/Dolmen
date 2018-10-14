/**
 * This package contains the lexer and parser definitions
 * for the simple Dolmen grammar descriptions.
 * <p>
 * Simple Dolmen grammar descriptions are a subset of
 * full-blown (also known as <i>extended</i>) grammar
 * descriptions and the first stage in the boot-strapping
 * process of parsing Dolmen language descriptions with
 * Dolmen itself.
 * <p>
 * The lexer for simple grammar descriptions is described
 * as a Dolmen lexer in {@code JGLexer.jl }, and
 * {@link jg.JGLexerStub} is then used to compile that
 * lexer into the {@link jg.JGLexer} Java class.
 * <p>
 * The parser for the simple grammar description is also
 * generated using Dolmen itself, but since no parser for
 * grammar descriptions exists at this point, its description
 * is constructed and provided declaratively in {@link jg.JGParser}
 * instead of being described using .jg syntax. That class'
 * {@link jg.JGParser#main(String[])} method can then be used
 * to turn that declarative grammar description into the
 * actual parser {@link jg.JGParserGenerated}.
 * <p>
 * The lexer and parsers obtained from this package can then
 * be used to process the grammar description for full-blown
 * Dolmen grammars, see the package {@link jge}.
 * <p>
 * <i>NB: The process of generating {@link jg.JGLexer} and
 * {@link jg.JGParser} is automated in the {@code build.xml}
 * ANT script through the eponym targets.</i>
 */
@org.eclipse.jdt.annotation.NonNullByDefault package jg;