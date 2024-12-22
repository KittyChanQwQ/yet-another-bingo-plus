import { defineConfig, HeadConfig } from 'vitepress'

const constants = {
  shortName: "Yet Another MC BINGO",
  name: "Yet Another Minecraft BINGO",
  description: "A multiplayer BINGO item hunt mod + lobby, compatible with vanilla clients.",
}

// https://vitepress.dev/reference/site-config
export default defineConfig({
  lang: "en-US",
  title: constants.shortName,
  description: constants.description,
  base: "/bingo/",
  head: [
    ["meta", { name: "theme-color", content: "#0f5e2f" }],
    ["meta", { property: "name", content: constants.name }],
    ["meta", { property: "keywords", content: "minigame,game,fabricmc,quiltmc,server,plugin" }],
    ["link", { rel: "icon", type: "image/svg+xml", href: "/bingo/favicon.svg" }],
    ["link", { rel: "icon", type: "image/png", href: "/bingo/favicon.png" }],
    ["meta", { property: "og:locale", content: "en" }],
    ["meta", { property: "og:site_name", content: constants.name }],
    ["meta", { property: "og:image", content: "https://minecraft.horrific.dev/bingo/screenshot.png" }],
    ["meta", { property: "twitter:card", content: "summary_large_image" }],
    ["meta", { property: "twitter:image", content: "https://minecraft.horrific.dev/bingo/screenshot.png" }],
  ],
  cleanUrls: true,
  themeConfig: {
    logo: "/logo.svg",
    // https://vitepress.dev/reference/default-theme-config
    editLink: {
      pattern: "https://gitlab.com/horrific-tweaks/bingo/-/blob/main/docs/:path",
    },
    nav: [
      { text: "Modrinth", link: "https://modrinth.com/mod/yet-another-minecraft-bingo" },
      {
        text: 'Links',
        items: [
          { text: "Releases", link: "https://modrinth.com/mod/yet-another-minecraft-bingo/versions" },
          { text: "Issue Tracker", link: "https://gitlab.com/horrific-tweaks/bingo/-/issues" },
        ]
      },
    ],

    sidebar: [
      {
        text: 'Introduction',
        collapsed: false,
        items: [
          { text: 'What is Bingo?', link: '/what-is-bingo' },
          { text: 'Getting Started', link: '/getting-started' },
          { text: 'Game Options', link: '/game-options' },
          { text: 'Command Cheatsheet', link: '/command-cheatsheet' },
        ],
      },
      {
        text: 'How to ...',
        collapsed: false,
        items: [
          { text: 'Set up a server', link: '/server-setup' },
          { text: 'Optimize the game', link: '/optimization' },
          { text: 'Add a custom lobby', link: '/lobby-structure' },
        ],
      },
      {
        text: 'Configuration',
        collapsed: false,
        items: [
          { text: 'Overview', link: '/config' },
          { text: 'config.json', link: '/config-extra' },
          { text: 'Functions', link: '/functions' },
          { text: 'Tier List Editor', link: '/tier-editor' },
        ],
      },
      {
        text: 'Compatible Mods',
        collapsed: true,
        items: [
          { text: 'JEI/REI/EMI', link: '/mods/items' },
          { text: 'Chunky', link: '/mods/chunky' },
          { text: 'LuckPerms', link: '/mods/luckperms' },
          { text: 'Vanish', link: '/mods/vanish' },
          { text: 'Voice Chat', link: '/mods/voice' },
        ]
      },
      {
        text: 'Datapack Guide',
        collapsed: true,
        items: [
          { text: 'Tier Lists', link: '/custom-tier-lists' },
          { text: 'Objectives', link: '/custom-objectives' },
          { text: 'Objective Icons', link: '/custom-objective-images' },
          { text: 'Function Tags', link: '/custom-function-tags' },
        ],
      },
      {
        text: 'Developer API',
        collapsed: true,
        items: [
          { text: 'Overview', link: '/api/overview' },
        ],
      },
    ],

    socialLinks: [
        { icon: { svg: "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\"><title>gitlab</title><path d=\"M21.94 13.11L20.89 9.89C20.89 9.86 20.88 9.83 20.87 9.8L18.76 3.32C18.65 3 18.33 2.75 17.96 2.75C17.6 2.75 17.28 3 17.17 3.33L15.17 9.5H8.84L6.83 3.33C6.72 3 6.4 2.75 6.04 2.75H6.04C5.67 2.75 5.35 3 5.24 3.33L3.13 9.82C3.13 9.82 3.13 9.83 3.13 9.83L2.06 13.11C1.9 13.61 2.07 14.15 2.5 14.45L11.72 21.16C11.89 21.28 12.11 21.28 12.28 21.15L21.5 14.45C21.93 14.15 22.1 13.61 21.94 13.11M8.15 10.45L10.72 18.36L4.55 10.45M13.28 18.37L15.75 10.78L15.85 10.45H19.46L13.87 17.61M17.97 3.94L19.78 9.5H16.16M14.86 10.45L13.07 15.96L12 19.24L9.14 10.45M6.03 3.94L7.84 9.5H4.23M3.05 13.69C2.96 13.62 2.92 13.5 2.96 13.4L3.75 10.97L9.57 18.42M20.95 13.69L14.44 18.42L14.46 18.39L20.25 10.97L21.04 13.4C21.08 13.5 21.04 13.62 20.95 13.69\" /></svg>" }, link: "https://gitlab.com/horrific-tweaks/bingo" },
        { icon: "discord", link: "https://discord.horrific.dev" },
    ],
  },
  transformHead({ pageData }) {
    const head: HeadConfig[] = [];

    if (!pageData.isNotFound) {
      const canonicalUrl = `https://minecraft.horrific.dev/bingo/${pageData.relativePath}`
        .replace(/\.md$/, '')
        .replace(/\/index$/, '/');

      head.push(['link', { rel: 'canonical', href: canonicalUrl }]);
      head.push(['meta', { property: 'og:url', content: canonicalUrl }]);

      const formattedTitle = pageData.title
        ? (
          canonicalUrl.endsWith("/")
            ? pageData.title
            : `${pageData.title} | ${constants.name}`
        )
        : constants.name;
      const description = pageData.description || constants.description;

      head.push(['meta', { name: 'description', content: description }]);
      head.push(['meta', { property: 'og:title', content: pageData.title || constants.name }]);
      head.push(['meta', { property: 'og:description', content: description }]);
      head.push(['meta', { property: 'og:type', content: canonicalUrl.endsWith("/") ? "website" : "article" }]);
      head.push(['meta', { property: 'twitter:title', content: formattedTitle }]);
      head.push(['meta', { property: 'twitter:description', content: description }]);
    }

    return head;
  },
})
