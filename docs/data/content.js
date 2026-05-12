// docs/data/content.js
export const technicalAnalysis = {
  // 1. Dex 数据恢复
  dexRecovery: {
    path: "guide/dex-recovery.md",
    title: "Dex 数据恢复研究",
    description: "针对 Epic 加固中 DEX 文件的保护机制，本项目研究了其 V1 与 V2 版本的差异化加密方案及静态还原路径。",
    sections: {
      v1Features: [
        "存储路径：加密后的 DEX 通常存放于 assets/Epic_dexs/ 目录下。",
        "加密算法：采用标准 RC4 流加密。",
        "密钥来源：密钥动态隐藏在 native 库的 .ArmEpic 节区中。"
      ],
      v2Features: [
        "存储路径：由配置文件中的 dex_dir 参数指定，通常位于 assets/ 的子目录下。",
        "文件后缀：加密文件通常以 .epic 作为后缀。",
        "加密算法：支持 RC4 (Method 1) 或单字节异或 (Method 0) 两种模式。"
      ],
      methods: [
        "配置提取：通过解析 ELF 文件的特定节区获取加固配置信息。",
        "参数识别：识别配置中的解密方法标识（Method）与对应的密钥（Key）。",
        "流式解密：遍历加密目录，根据文件序号（如 classes.dex, classes2.dex）进行流式还原。",
        "结构验证：还原后自动校验 DEX 文件头魔数（64 65 78），确保数据完整性。"
      ]
    }
  },

  // 2. Axml 资源修复
  axmlFix: {
    path: "guide/axml-fix.md",
    title: "Axml 资源修复研究",
    description: "Epic V2 对 Android 的二进制 XML（Axml）结构进行了破坏性保护，导致常规工具无法直接解析。",
    sections: {
      tactics: [
        "魔数篡改：将 Axml 标准头部魔数修改为 Epic (Hex: 45 70 69 63)。",
        "数据加密：XML 数据体部分经过 RC4 或异或加密。",
        "结构偏移：破坏了 Header 的固定结构，使解析器定位失败。"
      ],
      methods: [
        "特征扫描：遍历 /res 目录，通过文件头部的 Epic 标识定位受保护的 XML 文件。",
        "剥离与解密：保留头部标识，应用从配置中提取的 Axml 专用密钥进行解密。",
        "结构重组：将数据头部强制修正为标准 Axml 魔数（03 00 08 00），并填充资源索引表。",
        "打包校验：重新封装，确保 AXMLPrinter 等工具可正常读取。"
      ]
    }
  },

  // 3. Assets 资源还原
  assetsRestore: {
    path: "guide/assets-restore.md",
    title: "Assets 资源还原研究",
    description: "为了对抗资源提取，Epic V2 引入了资源重命名与加密存储的映射机制。",
    sections: {
      mechanisms: [
        "重命名映射：原始资源路径（Logic Path）映射为随机生成的真实路径（Real Path）。",
        "加密封装：资源内容根据全局配置进行二次加密。",
        "集中配置：映射关系均存储在加密的 JSON 配置块中。"
      ],
      methods: [
        "读取映射表：从 asset_rename_map 中提取路径对应关系。",
        "按需解密：根据参数选择 RC4 或异或解密逻辑。",
        "目录重构：解析逻辑路径层级，递归创建文件夹结构。",
        "批量还原：将解密后的数据流写回对应路径。"
      ]
    }
  },

  // 4. ELF 结构解析
  elfAnalysis: {
    path: "guide/elf-analysis.md",
    title: "ELF 结构解析研究",
    description: "本工具核心底层技术，用于从 native 库（libEPIC.so）中提取关键加密元数据。",
    sections: {
      sectionAnalysis: [
        "存储位置：存在于 ELF 文件的节区头部表（Section Header Table）中。",
        "V1 数据结构：紧凑二进制结构，含包名标识及 16 字节 RC4 密钥。",
        "V2 数据结构：密钥 + 加密 JSON 混合结构，包含业务逻辑 JSON 树。"
      ],
      workflow: [
        "架构自适应：自动处理 ELF32 (ARM) 与 ELF64 (AArch64) 的偏移差异。",
        "字符串表定位：通过 e_shstrndx 搜索 .ArmEpic 节区名。",
        "节区提取：根据 Shdr 记录的 offset 和 size 提取原始字节流。",
        "动态解密：利用元密钥解密获取最终配置树。"
      ]
    }
  }
};
