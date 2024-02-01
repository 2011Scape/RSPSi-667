package com.rspsi.plugin.loader;

import com.jagex.cache.config.VariableBits;
import com.jagex.cache.loader.config.VariableBitLoader;
import com.jagex.io.Buffer;
import org.apache.commons.compress.utils.Lists;
import org.displee.cache.index.Index;
import org.displee.cache.index.archive.Archive;
import org.displee.cache.index.archive.file.File;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//Checked
public class VarbitLoaderOSRS extends VariableBitLoader {

    private int count;
    private VariableBits[] bits;

    @Override
    public int count() {
        return count;
    }

    @Override
    public VariableBits forId(int id) {
        if (id < 0 || id > bits.length)
            return null;

        return bits[id];
    }

    @Override
    public void init(Archive archive) {

    }

    public void decodeVarbits(Index index) {
        int size = index.getArchives().length * 0x3ff;
        VariableBits[] varbits = new VariableBits[size];

        for(int i = 0; i < size; i++) {
            File file = index.getArchive(i >>> 10).getFile(i & 0x3FF);
            if (Objects.nonNull(file) && Objects.nonNull(file.getData())) {
                ByteBuffer buff = ByteBuffer.wrap(file.getData());
                VariableBits varbit = new VariableBits();
                while (true) {
                    int opcode = buff.get() & 0xff;
                    if (opcode == 0)
                        break;
                    if (opcode == 1) {
                        int setting = buff.getShort() & 0xffff;
                        int low = buff.get() & 0xff;
                        int high = buff.get() & 0xff;
                        varbit.setSetting(setting);
                        varbit.setHigh(high);
                        varbit.setLow(low);
                    }
                }
                varbits[i] = varbit;
            }
        }
        bits = varbits;
        this.count = bits.length;
        System.out.println("varbit count: " + this.count);
    }

    @Override
    public void init(Buffer arg0, Buffer arg1) {

    }

}
