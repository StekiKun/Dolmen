/**
 * This package contains the lexer and parser definitions
 * for the full-blown (or <i>extended</i>) Dolmen grammar 
 * descriptions.
 * <p>
 * These Dolmen grammar descriptions are called extended
 * in comparison with the simple grammar descriptions
 * from package {@link org.stekikun.dolmen.jg}, which only allow a subset
 * of the features supported by Dolmen. The point of
 * the simple grammar parsers is to be able to use
 * Dolmen to produce the parser for .jg files in a
 * boot-strapping fashion, at the cost of only being
 * able to use the features from simple grammars.
 * <p>
 * The lexer for simple grammar descriptions is described
 * as a Dolmen lexer in {@code JGELexer.jl }, and
 * {@link org.stekikun.dolmen.jge.JGELexerStub} is then used to compile that
 * lexer into the {@link org.stekikun.dolmen.jge.JGELexer} Java class.
 * <p>
 * The parser for grammar descriptions is described
 * as a simple Dolmen grammar in {@code JGEParser.jg},
 * and {@link org.stekikun.dolmen.jge.JGEParserStub} is then used to compile
 * that parser description into the {@link org.stekikun.dolmen.jge.JGEParser}
 * Java class.
 * <p>
 * <i>NB: The process of generating {@link org.stekikun.dolmen.jge.JGELexer} and
 * {@link org.stekikun.dolmen.jge.JGEParser} is automated in the {@code build.xml}
 * ANT script through the eponym targets.</i>
 */
@org.eclipse.jdt.annotation.NonNullByDefault package org.stekikun.dolmen.jge;