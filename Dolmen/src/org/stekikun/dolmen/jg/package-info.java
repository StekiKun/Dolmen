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
 * {@link org.stekikun.dolmen.jg.JGLexerStub} is then used to compile that
 * lexer into the {@link org.stekikun.dolmen.jg.JGLexer} Java class.
 * <p>
 * The parser for the simple grammar description is also
 * generated using Dolmen itself, but since no parser for
 * grammar descriptions exists at this point, its description
 * is constructed and provided declaratively in {@link org.stekikun.dolmen.jg.JGParser}
 * instead of being described using .jg syntax. That class'
 * {@link org.stekikun.dolmen.jg.JGParser#main(String[])} method can then be used
 * to turn that declarative grammar description into the
 * actual parser {@link org.stekikun.dolmen.jg.JGParserGenerated}.
 * <p>
 * The lexer and parsers obtained from this package can then
 * be used to process the grammar description for full-blown
 * Dolmen grammars, see the package {@link org.stekikun.dolmen.jge}.
 * <p>
 * <i>NB: The process of generating {@link org.stekikun.dolmen.jg.JGLexer} and
 * {@link org.stekikun.dolmen.jg.JGParserGenerated} is automated in the {@code build.xml}
 * ANT script through the eponym targets.</i>
 */
@org.eclipse.jdt.annotation.NonNullByDefault package org.stekikun.dolmen.jg;