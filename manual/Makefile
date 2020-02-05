SOURCES=manual.adoc LEXER-syntax.adoc PARSER-syntax.adoc index.adoc
HTMLS=$(SOURCES:.adoc=.html)
XMLS=$(SOURCES:.adoc=.xml)

ADOC=asciidoctor

.PHONY: all
all:	html xml
	cp *.html ../docs/

.PHONY: html
html:	$(HTMLS)
.PHONY: xml
xml:	$(XMLS)

%.html: %.adoc
	$(ADOC) $<

%.xml: %.adoc
	$(ADOC) -b docbook5 $<

.PHONY: clean
clean:
	rm -f *~ $(HTMLS) $(XMLS)

# Dependencies
$(HTMLS) $(XMLS): config.adoc
manual.html manual.xml: intro.adoc lexers.adoc lexers-tutorial.adoc LEXER-syntax.adoc parsers.adoc PARSER-syntax.adoc
LEXER-syntax.html LEXER-syntax.xml: config.adoc LEXER-syntax.adoc
PARSER-syntax.html PARSER-syntax.xml: config.adoc PARSER-syntax.adoc
index.html: config.adoc
