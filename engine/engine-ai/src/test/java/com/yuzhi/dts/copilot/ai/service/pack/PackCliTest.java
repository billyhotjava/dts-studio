package com.yuzhi.dts.copilot.ai.service.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;
import static org.assertj.core.api.Assertions.*;

class PackCliTest {
    @TempDir Path directory;

    @Test void buildValidateIsDeterministicAndNeverOverwrites() throws Exception {
        Path input=directory.resolve("source"); Files.createDirectory(input);
        try(var zip=new ZipInputStream(new ByteArrayInputStream(PackArchiveValidatorTest.pack("fixture","1.0.0","{\"domain\":\"d\",\"objects\":[]}")))) {
            java.util.zip.ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                Path file=input.resolve(entry.getName()); Files.createDirectories(file.getParent()); Files.write(file,zip.readAllBytes());
            }
        }
        Path one=directory.resolve("one.dtspack"),two=directory.resolve("two.dtspack");
        assertThat(PackCli.run(new String[]{"build",input.toString(),"-o",one.toString(),"--strict"})).isZero();
        assertThat(PackCli.run(new String[]{"build",input.toString(),"-o",two.toString()})).isZero();
        assertThat(Files.readAllBytes(one)).isEqualTo(Files.readAllBytes(two));
        assertThat(PackCli.run(new String[]{"validate",one.toString(),"--strict"})).isZero();
        assertThat(PackCli.run(new String[]{"build",input.toString(),"-o",one.toString()})).isEqualTo(1);
        Files.createSymbolicLink(input.resolve("link"),one);
        assertThat(PackCli.run(new String[]{"build",input.toString(),"-o",directory.resolve("unsafe.dtspack").toString()})).isEqualTo(1);
    }
}
