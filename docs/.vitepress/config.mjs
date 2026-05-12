import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "/EpicTool/",
  description: "Epic 加固分析与研究",
  lang: 'zh-CN',
  themeConfig: {
    // 顶部导航栏
    nav: [
      { text: '指南', link: '/guide/getting-started' },
      { text: 'GitHub', link: 'https://github.com/XingHuaiYa/EpicTool' }
    ],

    // 侧边栏配置
    sidebar: [
      {
        text: '基础指南',
        items: [
          { text: '项目介绍', link: '/guide/getting-started' },
          { text: '使用说明', link: '/guide/usage' },
        ]
      },
      {
        text: '技术研究',
        items: [
          { text: 'DEX 数据恢复', link: '/guide/dex-recovery' },
          { text: 'AXML 资源修复', link: '/guide/axml-fix' },
          { text: 'Assets 资源还原', link: '/guide/assets-restore' },
          { text: 'ELF 结构解析', link: '/guide/elf-analysis' },
        ]
      },
      {
        text: '其他',
        items: [
          { text: '免责声明', link: '/guide/disclaimer' }
        ]
      }
    ],

    // 社交链接
    socialLinks: [
      { icon: 'github', link: 'https://github.com/XingHuaiYa/EpicTool' }
    ],

    // 页脚配置
    footer: {
      message: '基于研究目的开发',
      copyright: 'Copyright © 2026-present XingHuaiYa'
    }
  }
})
