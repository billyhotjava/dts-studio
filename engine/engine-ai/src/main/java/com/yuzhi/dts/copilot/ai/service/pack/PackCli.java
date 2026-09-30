package com.yuzhi.dts.copilot.ai.service.pack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Standalone CLI; never starts Spring, connects to a database or invokes an LLM. */
public final class PackCli {
    private PackCli() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String[] args) {
        try {
            var arguments = new ArrayList<>(java.util.List.of(args));
            boolean strict = arguments.remove("--strict");
            if (arguments.size() < 2 || !java.util.Set.of("validate", "build").contains(arguments.getFirst())) {
                System.err.println("Usage: pack-cli validate <directory|archive> [--strict] | build <directory> -o <archive>");
                return 1;
            }
            boolean build = arguments.getFirst().equals("build");
            Path input = Path.of(arguments.get(1)).toAbsolutePath().normalize();
            if (build && (arguments.size() != 4 || !arguments.get(2).equals("-o"))) return 1;
            if (!build && arguments.size() != 2) return 1;
            byte[] archive;
            if (Files.isDirectory(input, LinkOption.NOFOLLOW_LINKS)) {
                archive = archive(input, build);
            } else {
                if (build || Files.isSymbolicLink(input)) return 1;
                try (var stream = Files.newInputStream(input)) { archive = stream.readNBytes(PackArchiveValidator.MAX_ARCHIVE + 1); }
            }
            var result = new PackArchiveValidator().validate(new ByteArrayInputStream(archive));
            if (strict && !result.warnings().isEmpty()) return 1;
            if (build) {
                Path output = Path.of(arguments.get(3)).toAbsolutePath().normalize();
                if (output.startsWith(input)) throw new IllegalArgumentException("Output must be outside the pack directory");
                Files.write(output, archive, java.nio.file.StandardOpenOption.CREATE_NEW);
            }
            System.out.printf("%s@%s: %d assets, %d warnings%n", result.name(), result.version(), result.assets().size(), result.warnings().size());
            return result.warnings().isEmpty() ? 0 : 2;
        } catch (PackException e) {
            System.err.println(e.code() + ": " + e.errors());
            return 1;
        } catch (Exception e) {
            System.err.println("Pack validation/build failed: " + e.getMessage());
            return 1;
        }
    }

    static byte[] archive(Path directory, boolean createHashes) throws Exception {
        TreeMap<String, byte[]> files = new TreeMap<>();
        long total = 0;
        try (var paths = Files.walk(directory)) {
            var iterator = paths.iterator();
            int count = 0;
            while (iterator.hasNext()) {
                Path path = iterator.next();
                if (++count > 4000) throw new IllegalArgumentException("Too many pack paths");
                if (Files.isSymbolicLink(path)) throw new IllegalArgumentException("Symlinks are not allowed");
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) continue;
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("Unsupported file type");
                String name = directory.relativize(path).toString().replace(java.io.File.separatorChar, '/');
                PackArchiveValidator.requireSafePath(name);
                if (createHashes && name.equals("SHA256SUMS")) continue;
                byte[] bytes;
                try (var stream = Files.newInputStream(path)) { bytes = stream.readNBytes(PackArchiveValidator.MAX_ENTRY + 1); }
                total += bytes.length;
                if (bytes.length > PackArchiveValidator.MAX_ENTRY || total > PackArchiveValidator.MAX_EXPANDED || files.size() >= 1999)
                    throw new IllegalArgumentException("Pack exceeds limits");
                files.put(name, bytes);
            }
        }
        if (createHashes) {
            StringBuilder sums = new StringBuilder();
            files.forEach((name, data) -> sums.append(PackArchiveValidator.sha256(data)).append("  ").append(name).append('\n'));
            files.put("SHA256SUMS", sums.toString().getBytes(StandardCharsets.UTF_8));
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (var file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTime(0);
                zip.putNextEntry(entry);
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        if (output.size() > PackArchiveValidator.MAX_ARCHIVE) throw new IllegalArgumentException("Archive exceeds limits");
        return output.toByteArray();
    }
}
