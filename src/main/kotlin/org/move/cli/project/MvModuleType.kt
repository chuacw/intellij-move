package org.move.cli.project

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import org.move.ide.MvIcons
import javax.swing.Icon

class MvModuleType: ModuleType<MvModuleBuilder>(ID) {
    override fun getNodeIcon(isOpened: Boolean): Icon = MvIcons.MOVE

    override fun createModuleBuilder(): MvModuleBuilder = MvModuleBuilder()

    override fun getDescription(): String = "Move module"

    override fun getName(): String = "Move"

    companion object {
        const val ID = "MOVE_MODULE"
        val INSTANCE: MvModuleType by lazy { ModuleTypeManager.getInstance().findByID(ID) as MvModuleType }
    }
}
