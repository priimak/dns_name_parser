# $Id: Makefile,v 1.8 2007/07/13 22:44:33 priimak Exp $
all: parser.jar test.class test

.PHONY: clean realclean dist docs

include SETTINGS

parser.jar: src/su/netdb/parser/Parser.class
	$(JAR) cvf parser.jar `find . -name '*.class'`

src/su/netdb/parser/Parser.class: src/su/netdb/parser/Parser.java
	$(JAVAC) -O -classpath $(OROJAR):. src/su/netdb/parser/Parser.java

test.class test: test.java
	$(JAVAC) -classpath parser.jar:src/:. test.java ;\
	/bin/echo -e "#!/bin/sh\n$(JVM) -cp $(OROJAR):parser.jar:src/:. test \$$1" > test ;\
	chmod 755 test

docs:
	$(JAVADOC) -classpath $(OROJAR) -d docs $(wildcard su/netdb/parser/*.java)

dist: clean
	cd ../ && \
	$(GNUTAR) zcf parser/parser.src.$(shell cat VERSION).tar.gz $(shell cd ../ && find parser/ -type f | grep -v CVS)

clean:
	find -name '*.class' -exec rm -f {} \;
	find -name 'parser.src.*.tar.gz' -exec rm -f {} \;
	rm -f parser.jar test

realclean: clean
	find -name '*.html' -exec rm -f {} \;
	find -name '*.css' -exec rm -f {} \;
	rm -rf docs/su docs/package-list \;

