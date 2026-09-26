JAVAC = javac
JAVA = java

SOURCES = $(wildcard *.java)
CLASSES = $(SOURCES:.java=.class)

.PHONY: all run test clean

all: $(CLASSES)

$(CLASSES): $(SOURCES)
	$(JAVAC) $(SOURCES)

run: all
	$(JAVA) Main

test: all
	$(JAVA) ListCorrectnessTest

clean:
	rm -f *.class
