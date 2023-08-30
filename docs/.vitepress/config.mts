import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "Yet Another Minecraft BINGO",
  description: "A server-side BINGO item hunt mod + lobby, compatible with vanilla clients.",
  base: "/bingo/",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Configuration Tool', link: '/config' },
    ],

    sidebar: [
      {
        text: 'Configuration Tool',
        items: []
      }
    ],

    socialLinks: [],
  }
})
