package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class InvalidReturnTypeTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test no return type but returns u8`() = checkErrors("""
    module M {
        fun call() {
            <error descr="Invalid return type 'integer', expected '()'">return 1</error>;
        }
    }    
    """)

    fun `test no return type but returns u8 with expression`() = checkErrors("""
    module M {
        fun call() {
            <error descr="Invalid return type 'integer', expected '()'">1</error>
        }
    }    
    """)

    fun `test if statement returns ()`() = checkErrors("""
    module M {
        fun m() {
            if (true) {1} else {2};
        }
    }    
    """)

    fun `test block expr returns ()`() = checkErrors("""
    module M {
        fun m() {
            {1};
        }
    }    
    """)
}
