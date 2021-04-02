CLASSES = Reduce.class Tally.class Data.class HW8.class HW8Serial.class
JAVAFLAGS = -J-Xmx48m

all: $(CLASSES)

%.class : %.java
	javac $(JAVAFLAGS) $<

clean:
	rm *.class
