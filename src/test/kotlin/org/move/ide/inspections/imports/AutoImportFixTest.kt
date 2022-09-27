package org.move.ide.inspections.imports

import org.intellij.lang.annotations.Language
import org.move.ide.inspections.MvUnresolvedReferenceInspection
import org.move.utils.tests.annotation.InspectionTestBase

class AutoImportFixTest : InspectionTestBase(MvUnresolvedReferenceInspection::class) {
    fun `test method`() = checkAutoImportFixByText(
        """
module 0x1::M {
    public fun call() {}
}
script {
    fun main() {
        <error descr="Unresolved reference: `call`">/*caret*/call</error>();
    }
}
    """,
        """
module 0x1::M {
    public fun call() {}
}
script {
    use 0x1::M::call;

    fun main() {
        call();
    }
}
    """
    )

    fun `test module`() = checkAutoImportFixByText(
        """
module 0x1::Signer {
    public fun address_of() {}
}
script {
    fun main() {
        <error descr="Unresolved reference: `Signer`">/*caret*/Signer</error>::address_of();
    }
}
    """,
        """
module 0x1::Signer {
    public fun address_of() {}
}
script {
    use 0x1::Signer;

    fun main() {
        Signer::address_of();
    }
}
    """
    )

    fun `test module in other module with struct`() = checkAutoImportFixByText(
        """
module 0x1::Module {
    public fun call() {} 
}
module 0x1::Main {
    struct BTC {}

    fun m() {
        <error descr="Unresolved reference: `Module`">/*caret*/Module</error>::call();
    }
}        
    """, """
module 0x1::Module {
    public fun call() {} 
}
module 0x1::Main {
    use 0x1::Module;

    struct BTC {}

    fun m() {
        Module::call();
    }
}        
    """
    )

    fun `test unavailable if unresolved member`() = checkAutoImportFixIsUnavailable(
        """
module 0x1::M {}
module 0x1::Main {
    use 0x1::M;
    
    fun main() {
        M::<error descr="Unresolved reference: `value`">/*caret*/value</error>();
    }
}        
    """
    )

    fun `test auto import to the same group`() = checkAutoImportFixByText(
        """
module 0x1::M {
    struct S {}
    public fun call() {}
}
module 0x1::Main {
    use 0x1::M::S;
    
    fun main() {
        <error descr="Unresolved reference: `call`">/*caret*/call</error>();
    }
}
    """, """
module 0x1::M {
    struct S {}
    public fun call() {}
}
module 0x1::Main {
    use 0x1::M::{S, call};
    
    fun main() {
        call();
    }
}
    """
    )

    fun `test multiple import candidates`() = checkAutoImportFixByTextWithMultipleChoice(
        """
module 0x1::M1 {
    public fun call() {}
}
module 0x1::M2 {
    public fun call() {}
}
module 0x1::Main {
    public fun main() {
        <error descr="Unresolved reference: `call`">/*caret*/call</error>();
    }
}
    """, setOf("0x1::M1::call", "0x1::M2::call"), "0x1::M1::call", """
module 0x1::M1 {
    public fun call() {}
}
module 0x1::M2 {
    public fun call() {}
}
module 0x1::Main {
    use 0x1::M1::call;

    public fun main() {
        call();
    }
}
    """
    )

    fun `test no struct in module context`() = checkAutoImportFixByText(
        """
module 0x1::Token {
    struct Token {}
    struct MintCapability {}
    public fun call() {}
}
module 0x1::Main {
    fun main(a: <error descr="Unresolved reference: `Token`">/*caret*/Token</error>::MintCapability) {}
}
    """, """
module 0x1::Token {
    struct Token {}
    struct MintCapability {}
    public fun call() {}
}
module 0x1::Main {
    use 0x1::Token;

    fun main(a: Token::MintCapability) {}
}
    """
    )

    fun `test struct same name as module import`() = checkAutoImportFixByText(
        """
module 0x1::Token {
    struct Token {}
    public fun call() {}
}
module 0x1::Main {
    use 0x1::Token;
    
    fun main(a: <error descr="Unresolved reference: `Token`">/*caret*/Token</error>) {
        Token::call();
    }
}
    """, """
module 0x1::Token {
    struct Token {}
    public fun call() {}
}
module 0x1::Main {
    use 0x1::Token;
    use 0x1::Token::Token;

    fun main(a: Token) {
        Token::call();
    }
}
    """
    )

    fun `test unresolved function on module should not have import fix`() = checkAutoImportFixIsUnavailable(
        """
    module 0x1::Coin {
        public fun initialize() {}
    }        
    module 0x1::AnotherCoin {}
    module 0x1::Main {
        use 0x1::AnotherCoin;
        
        fun call() {
            AnotherCoin::<error descr="Unresolved reference: `initialize`">/*caret*/initialize</error>();
        }
    }
    """
    )

    fun `test test_only function available in test_only module`() = checkAutoImportFixByText(
        """
    module 0x1::Minter {
        #[test_only]
        public fun get_weekly_emission(): u64 { 0 }
    }   
    #[test_only] 
    module 0x1::MinterTests {
        #[test]
        fun test_a() {
            <error descr="Unresolved reference: `get_weekly_emission`">/*caret*/get_weekly_emission</error>();
        }    
    }
    """, """
    module 0x1::Minter {
        #[test_only]
        public fun get_weekly_emission(): u64 { 0 }
    }   
    #[test_only] 
    module 0x1::MinterTests {
        use 0x1::Minter::get_weekly_emission;

        #[test]
        fun test_a() {
            get_weekly_emission();
        }    
    }
    """
    )

    fun `test test_only function available in test function with test_only import`() =
        checkAutoImportFixByText(
            """
    module 0x1::Minter {
        #[test_only]
        public fun get_weekly_emission(): u64 { 0 }
    }   
    module 0x1::MinterTests {
        #[test]
        fun my_fun() {
            <error descr="Unresolved reference: `get_weekly_emission`">/*caret*/get_weekly_emission</error>();
        }    
    }
    """, """
    module 0x1::Minter {
        #[test_only]
        public fun get_weekly_emission(): u64 { 0 }
    }   
    module 0x1::MinterTests {
        #[test_only]
        use 0x1::Minter::get_weekly_emission;

        #[test]
        fun my_fun() {
            get_weekly_emission();
        }    
    }
    """
        )

    fun `test auto import add non-test-only statement`() = checkAutoImportFixByText("""
module 0x1::minter {
    public fun mint() {}    
}        
module 0x1::main {
    #[test_only]
    use 0x1::minter::mint;
    
    public fun main() {
        <error descr="Unresolved reference: `mint`">/*caret*/mint</error>();    
    }
    
    #[test_only]
    public fun main_test() {
        mint();
    }
}        
    """, """
module 0x1::minter {
    public fun mint() {}    
}        
module 0x1::main {
    #[test_only]
    use 0x1::minter::mint;
    use 0x1::minter::mint;

    public fun main() {
        mint();    
    }
    
    #[test_only]
    public fun main_test() {
        mint();
    }
}        
    """)

    fun `test auto import adds non test_only item`() = checkAutoImportFixByText("""
module 0x1::minter {
    struct S {}
    public fun mint() {}    
}        
module 0x1::main {
    #[test_only]
    use 0x1::minter::{Self, mint};
    
    public fun main() {
        <error descr="Unresolved reference: `mint`">/*caret*/mint</error>();    
    }
    
    #[test_only]
    public fun main_test(): minter::S {
        mint();
    }
}        
    """, """
module 0x1::minter {
    struct S {}
    public fun mint() {}    
}        
module 0x1::main {
    #[test_only]
    use 0x1::minter::{Self, mint};
    use 0x1::minter::mint;

    public fun main() {
        mint();    
    }
    
    #[test_only]
    public fun main_test(): minter::S {
        mint();
    }
}        
    """)

    private fun checkAutoImportFixByText(
        @Language("Move") before: String,
        @Language("Move") after: String,
    ) = doTest { checkFixByText(AutoImportFix.NAME, before, after) }

    private fun checkAutoImportFixIsUnavailable(@Language("Move") text: String) =
        doTest { checkFixIsUnavailable(AutoImportFix.NAME, text) }

    protected fun checkAutoImportFixByTextWithMultipleChoice(
        @Language("Move") before: String,
        expectedElements: Set<String>,
        choice: String,
        @Language("Move") after: String
    ) = doTest {
        var chooseItemWasCalled = false

        withMockImportItemUi(object : ImportItemUi {
            override fun chooseItem(items: List<ImportCandidate>, callback: (ImportCandidate) -> Unit) {
                chooseItemWasCalled = true
                val actualItems = items.mapTo(HashSet()) { it.fqPath.toString() }
                assertEquals(expectedElements, actualItems)
                val selectedValue = items.find { it.fqPath.toString() == choice }
                    ?: error("Can't find `$choice` in `$actualItems`")
                callback(selectedValue)
            }
        }) { checkFixByText(AutoImportFix.NAME, before, after) }

        check(chooseItemWasCalled) { "`chooseItem` was not called" }
    }

    private inline fun doTest(action: () -> Unit) {
        val inspection = inspection as MvUnresolvedReferenceInspection
        val defaultValue = inspection.ignoreWithoutQuickFix
        try {
            inspection.ignoreWithoutQuickFix = false
            action()
        } finally {
            inspection.ignoreWithoutQuickFix = defaultValue
        }
    }
}