import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  lang: "en-US",
  title: "Yet Another MC BINGO",
  description: "A server-side BINGO item hunt mod + lobby, compatible with vanilla clients.",
  base: "/bingo/",
  head: [
    ["meta", { name: "theme-color", content: "#0f5e2f" }],
    ["link", { rel: "icon", type: "image/svg+xml", href: "/bingo/favicon.svg" }],
    ["link", { rel: "icon", type: "image/png", href: "/bingo/favicon.png" }],
  ],
  themeConfig: {
    logo: "/logo.svg",
    // https://vitepress.dev/reference/default-theme-config
    editLink: {
      pattern: "https://gitlab.com/horrific-tweaks/bingo/-/blob/main/docs/:path",
    },
    nav: [
      { text: 'Documentation', link: '/config' },
      {
        text: 'Links',
        items: [
          { text: "Releases", link: "https://modrinth.com/mod/yet-another-minecraft-bingo/versions" },
          { text: "Source Code", link: "https://gitlab.com/horrific-tweaks/bingo" }
        ]
      },
    ],

    sidebar: [
      {
        text: 'Configuration',
        items: [
          { text: 'Editor', link: '/config' },
          { text: 'Item Distribution', link: '/item-distribution' },
          { text: 'Item Images', link: '/item-images' },
          { text: 'Lobby Structure', link: '/lobby-structure' },
        ],
      },
    ],

    socialLinks: [],
  }
})
