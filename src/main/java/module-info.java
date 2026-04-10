open module org.openmarkov.learning.core {
	requires org.openmarkov.core;
	requires commons.math3;
    requires org.jetbrains.annotations;
    requires org.apache.logging.log4j;
    
    exports org.openmarkov.learning.core;
	exports org.openmarkov.learning.core.algorithm;
	exports org.openmarkov.learning.core.util;
	exports org.openmarkov.learning.core.exception;
	exports org.openmarkov.learning.core.preprocess;

}
