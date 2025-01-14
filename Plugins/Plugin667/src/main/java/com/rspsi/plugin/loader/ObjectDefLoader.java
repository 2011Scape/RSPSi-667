package com.rspsi.plugin.loader;

import com.google.common.collect.Maps;
import com.jagex.Client;
import com.jagex.cache.config.VariableBits;
import com.jagex.cache.def.ObjectDefinition;
import com.jagex.cache.loader.config.VariableBitLoader;
import com.jagex.cache.loader.object.ObjectDefinitionLoader;
import com.jagex.io.Buffer;
import com.jagex.util.ByteBufferUtils;
import lombok.extern.slf4j.Slf4j;
import org.displee.cache.index.Index;
import org.displee.cache.index.archive.Archive;
import org.displee.cache.index.archive.file.File;
import org.displee.utilities.Miscellaneous;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class ObjectDefLoader extends ObjectDefinitionLoader {

	private Map<Integer, ObjectDefinition> definitions = Maps.newHashMap();
	
	@Override
	public void init(Archive archive) {

	}
	
	@Override
	public void init(Buffer data, Buffer indexBuffer) {

	}

	private int size;

	public void decodeObjects(Index index) {
		size = index.getLastArchive().getId() * 256 + index.getLastArchive().getLastFile().getId();
		for (int id = 0; id < size; id++) {
			int archiveId = Miscellaneous.getConfigArchive(id, 8);
			Archive archive = index.getArchive(archiveId);
			if (Objects.nonNull(archive)) {
				int fileId = Miscellaneous.getConfigFile(id, 8);
				File file = archive.getFile(fileId);
				if (Objects.nonNull(file) && Objects.nonNull(file.getData())) {
					try {
						ObjectDefinition def = decode(id, ByteBuffer.wrap(file.getData()));
						definitions.put(id, def);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}

	public ObjectDefinition decode(int id, ByteBuffer buffer) {
		ObjectDefinition definition = new ObjectDefinition();
		definition.reset();
		definition.setId(id);
		int interactive = -1;
		int lastOpcode = -1;
		try {
			for (;;) {
				int opcode = buffer.get() & 0xff;

				if (opcode == 0)
					break;
				if (opcode == 1) {
					int typeSize = buffer.get() & 0xff;
					int[][] modelIds = new int[typeSize][];
					int[] modelTypes = new int[typeSize];
					for (int type = 0; type < typeSize; type++) {
						modelTypes[type] = buffer.get();
						int modelsLength = buffer.get() & 0xff;
						modelIds[type] = new int[modelsLength];
						for (int model = 0; modelsLength > model; model++) {
							modelIds[type][model] = buffer.getShort() & 0xffff;
						}
					}
					definition.setModelIds(IntStream.range(0, modelIds.length).map(index -> modelIds[index][0]).toArray());
					definition.setModelTypes(modelTypes);
				} else if (opcode == 5) {
					int typeSize = buffer.get() & 0xff;
					int[][] modelIds = new int[typeSize][];
					int[] modelTypes = new int[typeSize];
					for (int type = 0; type < typeSize; type++) {
						modelTypes[type] = buffer.get();
						int modelsLength = buffer.get() & 0xff;
						modelIds[type] = new int[modelsLength];
						for (int model = 0; modelsLength > model; model++) {
							modelIds[type][model] = buffer.getShort() & 0xffff;
						}
					}
					definition.setModelIds(IntStream.range(0, modelIds.length).map(index -> modelIds[index][0]).toArray());
					definition.setModelTypes(modelTypes);

					int length = buffer.get() & 0xff;
					for (int index = 0; index < length; index++) {
						buffer.get();
						int length2 = buffer.get() & 0xff;
						for (int i = 0; i < length2; i++) {
							int s = buffer.getShort() & 0xff;
						}
					}
				} else if (opcode == 2) {
					definition.setName(ByteBufferUtils.getOSRSString(buffer));
				} else if (opcode == 14) {
					definition.setWidth(buffer.get() & 0xff);
				} else if (opcode == 15) {
					definition.setLength(buffer.get() & 0xff);
				} else if (opcode == 17) {
					definition.setSolid(false);
				} else if (opcode == 18) {
					definition.setImpenetrable(false);
				} else if (opcode == 19) { // x
					interactive = buffer.get() & 0xff;
					if (interactive == 1) {
						definition.setInteractive(true);
					}
				} else if (opcode == 21) { // x
					definition.setContouredGround(true);
				} else if (opcode == 22) {
					definition.setDelayShading(true);
				} else if (opcode == 23) {
					definition.setOccludes(true);
				} else if (opcode == 24) {
					int animation = buffer.getShort() & 0xffff;
					if (animation == 65535) {
						animation = -1;
					}
					definition.setAnimation(-1);
				} else if (opcode == 27) { // x
					//setInteractType(1);
				} else if (opcode == 28) { // x
					definition.setDecorDisplacement((buffer.get() & 0xff) << 2);
				} else if (opcode == 29) {
					definition.setAmbientLighting((byte) (buffer.get()));
				} else if (opcode == 39) {
					definition.setLightDiffusion((byte) (buffer.get()));
				} else if (opcode >= 30 && opcode < 39) {
					String[] interactions = new String[10];
					interactions[opcode - 30] = ByteBufferUtils.getOSRSString(buffer);
					if (interactions[opcode - 30].equalsIgnoreCase("hidden")) {
						interactions[opcode - 30] = null;
					}
					definition.setInteractions(interactions);
				} else if (opcode == 40) { // x
					int count = buffer.get() & 0xff;
					int[] originalColours = new int[count];
					int[] replacementColours = new int[count];
					for (int i = 0; i < count; i++) {
						originalColours[i] = buffer.getShort() & 0xffff;
						replacementColours[i] = buffer.getShort() & 0xffff;
					}
					definition.setOriginalColours(originalColours);
					definition.setReplacementColours(replacementColours);
				} else if (opcode == 41) {
					int i = buffer.get() & 0xff;
					for (int x = 0; x < i; x++) {
						int i1 = buffer.getShort() & 0xffff;
						int i2 = buffer.getShort() & 0xffff;
					}
				} else if (opcode == 42) {
					int i = buffer.get() & 0xff;
					for (int index = 0; index < i; index++)
						buffer.get();
				} else if (opcode == 44) {
					int i = buffer.getShort() & 0xffff;
				} else if (opcode == 45) {
					int i = buffer.getShort() & 0xffff;
				} else if (opcode == 60) {
					definition.setMinimapFunction(buffer.getShort() & 0xffff);
				} else if (opcode == 62) {
					definition.setInverted(true);
				} else if (opcode == 64) {
					definition.setCastsShadow(false);
				} else if (opcode == 65) { // x
					definition.setScaleX(buffer.getShort() & 0xffff);
				} else if (opcode == 66) {
					definition.setScaleY(buffer.getShort() & 0xffff);
				} else if (opcode == 67) {
					definition.setScaleZ(buffer.getShort() & 0xffff);
				} else if (opcode == 68) {
					definition.setMapscene(buffer.getShort() & 0xffff);
				} else if (opcode == 69) { // x
					definition.setSurroundings(buffer.get() & 0xff);
				} else if (opcode == 70) {
					definition.setTranslateX(buffer.getShort() & 0xffff);
				} else if (opcode == 71) { // x
					definition.setTranslateY(buffer.getShort() & 0xffff);
				} else if (opcode == 72) {
					definition.setTranslateZ(buffer.getShort() & 0xffff);
				} else if (opcode == 73) { // x
					definition.setObstructsGround(true);
				} else if (opcode == 74) { // x
					definition.setHollow(true);
				} else if (opcode == 75) {
					definition.setSupportItems(buffer.get() & 0xff);
				} else if (opcode == 77 || opcode == 92) {
					int varbit = buffer.getShort() & 0xffff;
					if (varbit == 65535) {
						varbit = -1;
					}
					int varp = buffer.getShort() & 0xffff;
					if (varp == 65535) {
						varp = -1;
					}
					int var3 = -1;
					if (opcode == 92) {
						var3 = buffer.getShort() & 0xffff;
						if (var3 == 65535)
							var3 = -1;
					}
					int count = buffer.get() & 0xff;
					int[] morphisms = new int[count + 2];
					for (int i = 0; i <= count; i++) {
						morphisms[i] = buffer.getShort() & 0xffff;
						if (morphisms[i] == 65535) {
							morphisms[i] = -1;
						}
					}
					morphisms[count + 1] = var3;
					definition.setMorphisms(morphisms);
					definition.setVarbit(varbit);
					definition.setVarp(varp);
				} else if (opcode == 78) { // x
					buffer.getShort();
					buffer.get();
				} else if (opcode == 79) {
					buffer.getShort();
					buffer.getShort();
					buffer.get();
					int count = buffer.get();
					for (int index = 0; index < count; index++)
						buffer.getShort();
				} else if (opcode == 81) { // x
					buffer.get();
				} else if(opcode == 82) {
					// aBoolean3891 = true;
				} else if(opcode == 88) {
					//aBoolean3853 = false;
				} else if(opcode == 89) {
					//aBoolean3891 = true;
				} else if(opcode == 91) {
					//aBoolean3873 = true;
				} else if (opcode == 93) {
					buffer.getShort();
				} else if(opcode == 94) {
					//aByte3912 = (byte) 4;
				} else if (opcode == 95) {
					buffer.getShort();
				} else if(opcode == 97) {
					// aBoolean3866 = true;
				} else if(opcode == 98) {
					//aBoolean3923 = true;
				} else if (opcode == 99) {
					buffer.get();
					buffer.getShort();
				} else if (opcode == 100) {
					buffer.get();
					buffer.getShort();
				} else if (opcode == 101) {
					buffer.get();
				} else if (opcode == 102) {
					buffer.getShort();
				} else if(opcode == 103) {
					// thirdInt = 0;
				} else if (opcode == 104) {
					buffer.get();
				} else if (opcode == 106) {
					int size = buffer.get() & 0xff;
					for (int index = 0; index < size; index++) {
						int i = buffer.getShort() & 0xff;
						int i2 = buffer.get() & 0xff;
					}
				} else if (opcode == 107) {
					buffer.getShort();
				} else if (opcode >= 150 && opcode < 155) {
					ByteBufferUtils.getOSRSString(buffer);
				} else if (opcode == 160) {
					int size = buffer.get() & 0xff;
					for (int index = 0; index < size; index++) {
						buffer.getShort();
					}
				} else if (opcode == 162) {
					buffer.getInt();
				} else if (opcode == 163) {
					buffer.get();
					buffer.get();
					buffer.get();
					buffer.get();
				} else if (opcode == 164) {
					buffer.getShort();
				} else if (opcode == 165) {
					buffer.getShort();
				} else if (opcode == 166) {
					buffer.getShort();
				} else if (opcode == 167) {
					buffer.getShort();
				} else if(opcode == 168) {
					//aBoolean3894 = true;
				} else if(opcode == 169) {
					//aBoolean3845 = true;
				} else if (opcode == 170) {
					ByteBufferUtils.getSmart(buffer);
				} else if (opcode == 171) {
					ByteBufferUtils.getSmart(buffer);
				} else if (opcode == 173) {
					buffer.getShort();
					buffer.getShort();
				} else if(opcode == 177) {
					// boolean ub = true;
				} else if (opcode == 178) {
					buffer.get();
				} else if (opcode == 249) {
					int var1 = buffer.get() & 0xff;
					for (int var2 = 0; var2 < var1; var2++) {
						boolean b = (buffer.get() & 0xff) == 1;
						int var5 = ByteBufferUtils.readU24Int(buffer);
						if (b) {
							ByteBufferUtils.getOSRSString(buffer);
						} else {
							buffer.getInt();
						}
					}
				} else {
					System.out.println("id: " + id + " unknown opcode: " + opcode + " last opcode: " + lastOpcode);
				}
				lastOpcode = opcode;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (definition.isHollow()) {
			definition.setSolid(false);
			definition.setImpenetrable(false);
		}
		definition.setDelayShading(false);

		if (definition.getSupportItems() == -1) {
			definition.setSupportItems(definition.isSolid() ? 1 : 0);
		}
		return definition;
	}

	@Override
	public ObjectDefinition forId(int id) {
		if (definitions.containsKey(id))
			return definitions.get(id);
		return forId(1);
	}

	@Override
	public int count() {
		return definitions.size();
	}

	@Override
	public ObjectDefinition morphism(int id) {
		ObjectDefinition def = forId(id);
		int morphismIndex = -1;
		if (def.getVarbit() != -1) {
			VariableBits bits = VariableBitLoader.lookup(def.getVarbit());
			if(bits == null){
				log.info("varbit {} was null!", def.getVarbit());
				return null;
			}
			int variable = bits.getSetting();
			int low = bits.getLow();
			int high = bits.getHigh();
			int mask = Client.BIT_MASKS[high - low];
			morphismIndex = Client.getSingleton().settings[variable] >> low & mask;
		} else if (def.getVarp() != -1)
			morphismIndex = Client.getSingleton().settings[def.getVarp()];
		int var2;
		if(morphismIndex >= 0 && morphismIndex < def.getMorphisms().length) {
			var2 = def.getMorphisms()[morphismIndex];
		} else {
			var2 = def.getMorphisms()[def.getMorphisms().length - 1];
		}
		return var2 == -1 ? null : ObjectDefinitionLoader.lookup(var2);
	}


}
