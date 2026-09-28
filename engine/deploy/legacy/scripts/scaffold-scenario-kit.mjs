#!/usr/bin/env node

import { access, mkdir, readdir, readFile, rm, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";

const SCENE_CODE_PATTERN = /^[a-z][a-z0-9_]{1,47}$/;
const DEFAULT_TEMPLATE_DIR = "worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/templates";
const DEFAULT_OUTPUT_DIR = "worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/generated";

function parseArgs(argv) {
  const out = {
    sceneCode: "",
    domainName: "",
    owner: "data-platform",
    sourceCatalog: "mysql",
    sourceSchema: "rs_cloud_flower",
    warehouseCatalog: "postgres",
    warehouseSchema: "public",
    templateDir: DEFAULT_TEMPLATE_DIR,
    outputDir: DEFAULT_OUTPUT_DIR,
    force: false,
    dryRun: false,
    help: false,
  };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--scene-code") {
      out.sceneCode = String(argv[++i] || "");
    } else if (arg === "--domain-name") {
      out.domainName = String(argv[++i] || "");
    } else if (arg === "--owner") {
      out.owner = String(argv[++i] || "");
    } else if (arg === "--source-catalog") {
      out.sourceCatalog = String(argv[++i] || "");
    } else if (arg === "--source-schema") {
      out.sourceSchema = String(argv[++i] || "");
    } else if (arg === "--warehouse-catalog") {
      out.warehouseCatalog = String(argv[++i] || "");
    } else if (arg === "--warehouse-schema") {
      out.warehouseSchema = String(argv[++i] || "");
    } else if (arg === "--template-dir") {
      out.templateDir = String(argv[++i] || "");
    } else if (arg === "--output-dir") {
      out.outputDir = String(argv[++i] || "");
    } else if (arg === "--force") {
      out.force = true;
    } else if (arg === "--dry-run") {
      out.dryRun = true;
    } else if (arg === "--help" || arg === "-h") {
      out.help = true;
    }
  }
  return out;
}

function printHelp() {
  console.log(`Usage:
  node scripts/scaffold-scenario-kit.mjs --scene-code inventory --domain-name "库存" [--owner "data-platform"] [--force] [--dry-run]

Options:
  --scene-code          Lowercase dbt/domain code, e.g. inventory
  --domain-name         Chinese business domain name, e.g. 库存
  --owner               Governance owner, default: data-platform
  --source-catalog      Trino source catalog, default: mysql
  --source-schema       Trino source schema, default: rs_cloud_flower
  --warehouse-catalog   Trino warehouse catalog, default: postgres
  --warehouse-schema    Warehouse schema, default: public
  --template-dir        Template root
  --output-dir          Output root; generated scene is written under <output-dir>/<scene-code>
  --force               Replace existing generated scene directory
  --dry-run             Print planned files without writing
`);
}

function fail(message) {
  console.error(`[scenario-kit] ${message}`);
  process.exit(1);
}

async function exists(filePath) {
  try {
    await access(filePath);
    return true;
  } catch {
    return false;
  }
}

function toPascalCase(value) {
  return String(value || "")
    .split(/[^a-zA-Z0-9]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join("");
}

function toKebabCase(value) {
  return String(value || "").replaceAll("_", "-");
}

function replacements(args) {
  return {
    sceneCode: args.sceneCode,
    sceneCodeKebab: toKebabCase(args.sceneCode),
    sceneCodePascal: toPascalCase(args.sceneCode),
    domainName: args.domainName,
    owner: args.owner,
    sourceCatalog: args.sourceCatalog,
    sourceSchema: args.sourceSchema,
    warehouseCatalog: args.warehouseCatalog,
    warehouseSchema: args.warehouseSchema,
    stgPrefix: `${args.sceneCode}_stg`,
    dwdPrefix: `${args.sceneCode}_dwd`,
    dwsPrefix: `${args.sceneCode}_dws`,
    adsPrefix: `${args.sceneCode}_ads`,
  };
}

function render(input, values) {
  return Object.entries(values).reduce(
    (text, [key, value]) => text.replaceAll(`{{${key}}}`, String(value)),
    input,
  );
}

async function collectTemplates(templateRoot, relativeDir = "") {
  const absoluteDir = path.join(templateRoot, relativeDir);
  const entries = await readdir(absoluteDir);
  const files = [];
  for (const entry of entries) {
    const relativePath = path.join(relativeDir, entry);
    const absolutePath = path.join(templateRoot, relativePath);
    const info = await stat(absolutePath);
    if (info.isDirectory()) {
      files.push(...await collectTemplates(templateRoot, relativePath));
    } else if (entry.endsWith(".tmpl")) {
      files.push(relativePath);
    }
  }
  return files;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    printHelp();
    return;
  }
  if (!SCENE_CODE_PATTERN.test(args.sceneCode)) {
    fail("invalid --scene-code, expected /^[a-z][a-z0-9_]{1,47}$/");
  }
  if (!args.domainName.trim()) {
    fail("--domain-name is required");
  }

  const root = process.cwd();
  const templateRoot = path.resolve(root, args.templateDir);
  const outputRoot = path.resolve(root, args.outputDir);
  const sceneOutputDir = path.join(outputRoot, args.sceneCode);
  if (!await exists(templateRoot)) {
    fail(`template directory not found: ${templateRoot}`);
  }
  if (await exists(sceneOutputDir) && !args.force && !args.dryRun) {
    fail(`output already exists: ${sceneOutputDir} (use --force to replace)`);
  }

  const values = replacements(args);
  const templates = await collectTemplates(templateRoot);
  const renderedTargets = templates.map((relativeTemplatePath) => {
    const relativeTargetPath = render(relativeTemplatePath.replace(/\.tmpl$/, ""), values);
    return {
      source: path.join(templateRoot, relativeTemplatePath),
      target: path.join(sceneOutputDir, relativeTargetPath),
      relativeTargetPath,
    };
  });

  if (args.dryRun) {
    console.log("[scenario-kit] dry-run");
    console.log(`scene: ${args.sceneCode} (${args.domainName})`);
    for (const file of renderedTargets) {
      console.log(`would write: ${file.target}`);
    }
    return;
  }

  if (args.force && await exists(sceneOutputDir)) {
    await rm(sceneOutputDir, { recursive: true, force: true });
  }
  await mkdir(sceneOutputDir, { recursive: true });

  for (const file of renderedTargets) {
    const content = await readFile(file.source, "utf8");
    await mkdir(path.dirname(file.target), { recursive: true });
    await writeFile(file.target, render(content, values), "utf8");
  }

  console.log("[scenario-kit] generated scenario skeleton:");
  console.log(`- ${sceneOutputDir}`);
  console.log("");
  console.log("Next steps:");
  console.log("1. Fill source tables and caliber traps in catalog-domain.json.");
  console.log("2. Replace dbt placeholder SQL with real ODS/STG/DWD/DWS/ADS models.");
  console.log("3. Fill semantic pack fewShots and guardrails, then run route regression.");
}

void main();
