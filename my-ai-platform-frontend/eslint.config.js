/**
 * ESLint "flat" 配置文件 (eslint.config.js)。
 * 用于定义项目的代码检查规则，帮助保持代码质量和风格一致性。
 * 这个配置文件使用了 ESLint 最新的配置格式，一个包含多个配置对象的数组。
 * 集成了 JavaScript、TypeScript、Vue、JSON、Markdown 的 linting 规则，并接入了 Prettier 进行代码格式化。
 */
import js from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";
import pluginVue from "eslint-plugin-vue";
import json from "@eslint/json";
import markdown from "@eslint/markdown";
import { defineConfig } from "eslint/config";
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended';


export default defineConfig([
  { files: ["**/*.{js,mjs,cjs,ts,vue}"], plugins: { js }, extends: ["js/recommended"] },
  { files: ["**/*.{js,mjs,cjs,ts,vue}"], languageOptions: { globals: { ...globals.browser, ...globals.node } } },
  tseslint.configs.recommended,
  pluginVue.configs["flat/essential"],
  { files: ["**/*.vue"], languageOptions: { parserOptions: { parser: tseslint.parser } } },
  { files: ["**/*.json"], plugins: { json }, language: "json/json", extends: ["json/recommended"] },
  { files: ["**/*.json5"], plugins: { json }, language: "json/json5", extends: ["json/recommended"] },
  { files: ["**/*.md"], plugins: { markdown }, language: "markdown/gfm", extends: ["markdown/recommended"] },
  eslintPluginPrettierRecommended,
]);