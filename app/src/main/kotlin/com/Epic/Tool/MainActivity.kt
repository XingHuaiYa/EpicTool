package com.Epic.Tool


import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.experimental.xor

class MainActivity : AppCompatActivity() {

    private lateinit var etApkPath: EditText
    private lateinit var tvLog: TextView
    private lateinit var btnStart: Button
    private lateinit var btnSelect: Button
    private var loadingDialog: AlertDialog? = null

    private val outputRoot = "${Environment.getExternalStorageDirectory().absolutePath}/静态脱壳/Epic静态工具/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etApkPath = findViewById(R.id.et_apk_path)
        tvLog = findViewById(R.id.tv_log)
        btnStart = findViewById(R.id.btn_start)
        btnSelect = findViewById(R.id.btn_select)

        btnSelect.setOnClickListener {
            if (checkAllFilesPermission()) {
                MaterialFilePicker(this).show { file ->
                    etApkPath.setText(file.absolutePath)
                    addLog("载入: ${file.name}")
                }
            }
        }

        btnStart.setOnClickListener {
            val path = etApkPath.text.toString().trim()
            if (path.isEmpty()) return@setOnClickListener
            if (checkAllFilesPermission()) analyzeTask(path)
        }

        showUpdateLog()
    }

    private fun analyzeTask(apkPath: String) {
        showLoading("正在解析 Epic 配置...")
        Thread {
            try {
                ZipFile(File(apkPath)).use { zf ->
                    if (isAppProtected(zf)) {
                        handleWhitelistInterception()
                        return@Thread
                    }

                    var version = "未知"
                    var config: JSONObject? = null
                    var v1Key: ByteArray? = null

                    val armEpic = findArmEpicInZip(zf)
                    if (armEpic != null) {
                        val v2Key = armEpic.copyOfRange(0, 16)
                        val decData = rc4(armEpic.copyOfRange(16, armEpic.size), v2Key)
                        val jsonStr = String(decData, StandardCharsets.UTF_8)

                        if (jsonStr.contains("protection_flag")) {
                            version = "EpicV2"
                            config = JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1))
                        } else {
                            version = "EpicV1"
                            v1Key = extractV1Key(armEpic)
                        }
                    } else if (hasEpic3Feature(zf)) {
                        version = "Epic Apk加固3.0"
                    }

                    runOnUiThread {
                        loadingDialog?.dismiss()
                        if (version == "未知") {
                            addLog("未识别到加固特征")
                        } else {
                            showReportDialog(apkPath, version, config, v1Key)
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loadingDialog?.dismiss()
                    addLog("分析失败: ${e.message}")
                }
            }
        }.start()
    }

    private fun showReportDialog(path: String, ver: String, cfg: JSONObject?, v1Key: ByteArray?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_report, null)
        val tvInfo = view.findViewById<TextView>(R.id.tv_report_info)
        val cbDex = view.findViewById<MaterialCheckBox>(R.id.cb_dex)
        val cbAssets = view.findViewById<MaterialCheckBox>(R.id.cb_assets)
        val cbAxml = view.findViewById<MaterialCheckBox>(R.id.cb_axml)

        tvInfo.text = "文件名: ${File(path).name}\n检测版本: $ver"

        if (ver == "EpicV2" && cfg != null) {
            cbDex.visibility = if (cfg.has("dex_protection_method")) View.VISIBLE else View.GONE
            
            if (cfg.has("asset_protection_method")) {
                cbAssets.visibility = View.VISIBLE
                cbAssets.text = "Assets保护"
            } else {
                cbAssets.visibility = View.GONE
            }

            if (cfg.has("axml_protection_method")) {
                cbAxml.visibility = View.VISIBLE
                cbAxml.text = "Axml保护"
            } else {
                cbAxml.visibility = View.GONE
            }
        } else {
            cbAssets.visibility = View.GONE
            cbAxml.visibility = View.GONE
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("加固配置识别成功")
            .setView(view)
            .setCancelable(false)
            .setPositiveButton("立即导出") { _, _ ->
                executeProcess(path, ver, cfg, v1Key, cbDex.isChecked, cbAssets.isChecked, cbAxml.isChecked)
            }
            .setNegativeButton("取消", null).show()
    }

    private fun executeProcess(path: String, ver: String, cfg: JSONObject?, v1Key: ByteArray?, doDex: Boolean, doAssets: Boolean, doAxml: Boolean) {
        showLoading("正在导出并打包...")
        tvLog.text = ">>> 任务开始\n"

        Thread {
            val apkName = File(path).name
            val zipName = "${apkName.replace(".apk", "")}_处理完成_${System.currentTimeMillis() % 1000}.zip"
            val zipFile = File(outputRoot, zipName)
            ensureDir(outputRoot)

            val readme = StringBuilder().apply {
                append("Epic 静态工具 - 任务报告\n")
                append("==============================\n")
                append("时间: ${SimpleDateFormat("yyyy-MM-dd HH:ss", Locale.CHINA).format(Date())}\n")
                append("类型: $ver\n")
            }

            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    ZipFile(File(path)).use { zf ->
                        if (doDex) {
                            addLog("还原 DEX...")
                            val count = handleDexRestore(ver, cfg, v1Key, zf, zos)
                            readme.append("[√] DEX 还原数量: $count\n")
                        }

                        if (doAssets && cfg != null) {
                            addLog("还原 Assets...")
                            val count = handleAssetsRestore(cfg, zf, zos)
                            readme.append("[√] Assets 还原数量: $count\n")
                        }

                        if (doAxml && cfg != null) {
                            addLog("修复 AXML...")
                            val count = handleAxmlRestore(cfg, zf, zos)
                            readme.append("[√] AXML 修复数量: $count\n")
                        }
                    }
                    readme.append("\n------------------------------\n发布: MT论坛 (bbs.binmt.cc)\n转载留名谢谢")
                    writeToZip(zos, "解密说明.txt", readme.toString().toByteArray(StandardCharsets.UTF_8))
                }

                runOnUiThread {
                    loadingDialog?.dismiss()
                    addLog("\n[√] 导出完成！")
                    MaterialAlertDialogBuilder(this).setTitle("成功")
                        .setMessage("结果已导出到：\n${zipFile.absolutePath}")
                        .setPositiveButton("确定", null).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loadingDialog?.dismiss()
                    addLog("执行出错: ${e.message}")
                }
            }
        }.start()
    }

    // --- 核心算法 ---

    private fun handleDexRestore(ver: String, cfg: JSONObject?, v1Key: ByteArray?, zf: ZipFile, zos: ZipOutputStream): Int {
        var count = 0
        when (ver) {
            "EpicV2" -> {
                val m = cfg?.optInt("dex_protection_method", 1) ?: 1
                val keyStr = if (m == 1) cfg?.optString("dex_protection_rc4_key", "epic") else cfg?.optInt("dex_protection_xor_key", 0).toString()
                val kBytes = keyStr?.toByteArray() ?: "epic".toByteArray()
                val dexDir = cfg?.optString("dex_dir", "EPIC")?.lowercase() ?: "epic"

                zf.entries().asSequence().sortedBy { it.name }.forEach { ze ->
                    val lowerName = ze.name.lowercase()
                    if (lowerName.contains("/$dexDir/") && lowerName.endsWith(".epic")) {
                        val data = zf.getInputStream(ze).readBytes()
                        val dec = if (m == 1) rc4(data, kBytes) else xor(data, keyStr?.toInt() ?: 0)
                        writeToZip(zos, "dex/classes${if (++count == 1) "" else count}.dex", dec)
                        addLog("已解密 Dex: $count")
                    }
                }
            }
            "EpicV1" -> {
                val prefix = "assets/Epic_dexs/".lowercase()
                zf.entries().asSequence().sortedBy { it.name }.forEach { ze ->
                    if (ze.name.lowercase().startsWith(prefix)) {
                        val dec = rc4(zf.getInputStream(ze).readBytes(), v1Key!!)
                        writeToZip(zos, "dex/classes${if (++count == 1) "" else count}.dex", dec)
                        addLog("已解密 Dex: $count")
                    }
                }
            }
            "Epic Apk加固3.0" -> {
                zf.entries().asSequence().forEach { ze ->
                    if (ze.name.lowercase().endsWith(".epic")) {
                        val data = zf.getInputStream(ze).readBytes()
                        if (data.size > 8) {
                            val bKey = data[7]
                            val dec = ByteArray(data.size) { i -> data[i] xor bKey }
                            writeToZip(zos, "dex/classes${if (++count == 1) "" else count}.dex", dec)
                            addLog("已解密 Dex: $count")
                        }
                    }
                }
            }
        }
        return count
    }

    private fun handleAssetsRestore(cfg: JSONObject, zf: ZipFile, zos: ZipOutputStream): Int {
        var count = 0
        val m = cfg.optInt("asset_protection_method", 1)
        val renameMap = cfg.optJSONObject("asset_rename_map") ?: return 0
        val keyStr = if (m == 1) cfg.optString("asset_protection_rc4_key", "epic") else cfg.optInt("asset_protection_xor_key", 0).toString()
        val key = keyStr.toByteArray()

        renameMap.keys().forEach { logicPath ->
            val realPath = renameMap.getString(logicPath)
            zf.getEntry(realPath)?.let { ze ->
                val data = zf.getInputStream(ze).readBytes()
                val dec = if (m == 1) rc4(data, key) else xor(data, keyStr.toInt())
                writeToZip(zos, "assets/$logicPath", dec)
                addLog("已解密 Assets: $logicPath")
                count++
            }
        }
        return count
    }

    private fun handleAxmlRestore(cfg: JSONObject, zf: ZipFile, zos: ZipOutputStream): Int {
        var count = 0
        val m = cfg.optInt("axml_protection_method", 1)
        val keyStr = if (m == 1) cfg.optString("axml_protection_rc4_key", "epic") else cfg.optInt("axml_protection_xor_key", 0).toString()
        val key = keyStr.toByteArray()

        zf.entries().asSequence().forEach { ze ->
            val name = ze.name
            if (name.endsWith(".xml") && (name == "AndroidManifest.xml" || name.startsWith("res/"))) {
                val data = zf.getInputStream(ze).readBytes()
                if (data.size > 4 && data[0] == 'E'.toByte() && data[1] == 'p'.toByte() && data[2] == 'i'.toByte() && data[3] == 'c'.toByte()) {
                    var dec = if (m == 1) {
                        val payload = data.copyOfRange(4, data.size)
                        val decPart = rc4(payload, key)
                        ByteArray(4 + decPart.size).apply {
                            System.arraycopy(decPart, 0, this, 4, decPart.size)
                        }
                    } else {
                        xor(data, keyStr.toInt())
                    }

                    if (dec.size >= 4) {
                        dec[0] = 0x03; dec[1] = 0x00; dec[2] = 0x08; dec[3] = 0x00
                    }
                    writeToZip(zos, "axml_restored/$name", dec)
                    addLog("已解密 AXML: $name")
                    count++
                }
            }
        }
        return count
    }

    // --- 工具方法 ---

    private fun findArmEpicInZip(zf: ZipFile): ByteArray? {
        zf.entries().asSequence().forEach { ze ->
            if (ze.name.endsWith(".so")) {
                val section = findArmEpicSection(zf.getInputStream(ze).readBytes())
                if (section != null) return section
            }
        }
        return null
    }

    private fun findArmEpicSection(so: ByteArray): ByteArray? {
        return try {
            val bb = ByteBuffer.wrap(so).order(ByteOrder.LITTLE_ENDIAN)
            val is32 = so[4].toInt() == 1
            val shOff = if (is32) bb.getInt(0x20).toLong() else bb.getLong(0x28)
            val shNum = bb.getShort(if (is32) 0x30 else 0x3C).toInt() and 0xFFFF
            val shStrIdx = bb.getShort(if (is32) 0x32 else 0x3E).toInt() and 0xFFFF
            val shSize = if (is32) 40 else 64
            val strTabOff = if (is32) bb.getInt((shOff + (shStrIdx * shSize)).toInt() + 16).toLong() 
                            else bb.getLong((shOff + (shStrIdx * shSize)).toInt() + 24)
            
            for (i in 0 until shNum) {
                val off = (shOff + (i * shSize)).toInt()
                val nameIdx = bb.getInt(off)
                val sb = StringBuilder()
                var k = strTabOff.toInt() + nameIdx
                while (k < so.size && so[k].toInt() != 0) {
                    sb.append(so[k].toInt().toChar())
                    k++
                }
                if (sb.toString() == ".ArmEpic") {
                    val o = if (is32) bb.getInt(off + 16).toLong() else bb.getLong(off + 24)
                    val s = if (is32) bb.getInt(off + 20).toLong() else bb.getLong(off + 32)
                    return so.copyOfRange(o.toInt(), (o + s).toInt())
                }
            }
            null
        } catch (e: Exception) { null }
    }

    private fun rc4(data: ByteArray, key: ByteArray): ByteArray {
        val s = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            j = (j + s[i] + (key[i % key.size].toInt() and 0xFF)) and 0xFF
            val temp = s[i]; s[i] = s[j]; s[j] = temp
        }
        var i = 0; var k = 0
        return ByteArray(data.size) { n ->
            i = (i + 1) and 0xFF
            k = (k + s[i]) and 0xFF
            val temp = s[i]; s[i] = s[k]; s[k] = temp
            ((data[n].toInt() and 0xFF) xor s[(s[i] + s[k]) and 0xFF]).toByte()
        }
    }

    private fun xor(d: ByteArray, k: Int): ByteArray {
        return ByteArray(d.size) { i -> (d[i].toInt() xor (k and 0xFF)).toByte() }
    }

    private fun writeToZip(zos: ZipOutputStream, name: String, data: ByteArray) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(data)
        zos.closeEntry()
    }

    private fun ensureDir(dir: String) = File(dir).apply { if (!exists()) mkdirs() }

    private fun addLog(m: String) = runOnUiThread { tvLog.append("> $m\n") }

    private fun showLoading(msg: String) {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        v.findViewById<TextView>(R.id.loading_msg).text = msg
        loadingDialog = MaterialAlertDialogBuilder(this).setView(v).setCancelable(false).show()
    }

    private fun isAppProtected(zf: ZipFile): Boolean {
        val target = "EpicSafeWhite".map { (it.code + 1).toByte() }.toByteArray()
        return try {
            zf.entries().asSequence().filter { 
                it.name == "AndroidManifest.xml" || it.name.startsWith("assets/") || it.name.startsWith("res/") 
            }.count { ze ->
                zf.getInputStream(ze).use { it.skip(maxOf(0, ze.size - target.size)); it.readBytes().contentEquals(target) }
            } >= 2
        } catch (e: Exception) { false }
    }

    // 省略部分辅助方法 (extractV1Key, hasEpic3Feature, checkAllFilesPermission 等按原样转换)
    private fun extractV1Key(d: ByteArray): ByteArray? {
        return try {
            var p1 = -1; var p2 = -1
            for (i in 5 until d.size) if (d[i].toInt() == 0) { p1 = i; break }
            for (i in p1 + 1 until d.size) if (d[i].toInt() == 0) { p2 = i; break }
            d.copyOfRange(p2 + 1, p2 + 17)
        } catch (e: Exception) { null }
    }

    private fun hasEpic3Feature(zf: ZipFile) = zf.entries().asSequence().any { it.name.lowercase().endsWith(".epic") }

    private fun checkAllFilesPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(i)
            return false
        }
        return true
    }

    private fun handleWhitelistInterception() = runOnUiThread {
        loadingDialog?.dismiss()
        MaterialAlertDialogBuilder(this).setTitle("处理终止").setMessage("受保护应用停止处理。").setPositiveButton("退出") { _, _ -> finish() }.show()
    }

    private fun showUpdateLog() {
        val log = "v1.26.3\n1. 存放逻辑重构：优化输出目录结构。\n2. 代码重构转用Kotlin\n3. 新增 Assets/Axml 保护还原。"
        MaterialAlertDialogBuilder(this).setTitle("更新公告").setMessage(log).setPositiveButton("OK", null).show()
    }

    // MaterialFilePicker 的 Kotlin 实现也同样简洁
    private class MaterialFilePicker(val activity: MainActivity) {
        private var currentPath = Environment.getExternalStorageDirectory()
        fun show(cb: (File) -> Unit) {
            val list = currentPath.listFiles()?.filter { it.isDirectory || it.name.lowercase().endsWith(".apk") }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
            
            val isRoot = currentPath == Environment.getExternalStorageDirectory()
            val names = mutableListOf<String>().apply {
                if (!isRoot) add("../")
                addAll(list.map { if (it.isDirectory) "📁 ${it.name}" else "📦 ${it.name}" })
            }

            MaterialAlertDialogBuilder(activity).setTitle(currentPath.absolutePath)
                .setItems(names.toTypedArray()) { _, w ->
                    var idx = w
                    if (!isRoot) {
                        if (idx == 0) { currentPath = currentPath.parentFile; return@setItems show(cb) }
                        idx--
                    }
                    val sel = list[idx]
                    if (sel.isDirectory) { currentPath = sel; show(cb) } else cb(sel)
                }.setNegativeButton("取消", null).show()
        }
    }
}
