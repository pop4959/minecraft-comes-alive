package net.mca.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.mca.entity.CribWoodType;
import net.mca.item.CribItem;
import net.mca.item.ItemsMCA;
import net.minecraft.util.DyeColor;

public class CribLanguageProvider extends FabricLanguageProvider {

	protected CribLanguageProvider(FabricDataOutput dataOutput) { super(dataOutput); }

	@Override
	public void generateTranslations(TranslationBuilder translationBuilder)
	{
		for(CribWoodType wood : CribWoodType.values())
		{
			for(DyeColor color : DyeColor.values())
			{
				CribItem item = (CribItem) ItemsMCA.CRIBS.stream().filter(c ->
				{
					CribItem crib = (CribItem) c.get();
					return crib.getColor() == color && crib.getWood() == wood;
				}).findFirst().get().get();
				
				String colorName = "";
				for(String s : item.getColor().getName().split("_"))
				{
					colorName += ( s.substring(0, 1).toUpperCase() + s.substring(1, s.length()) + " ");
				}
				
				String woodName = "";
				for(String s : item.getWood().toString().toLowerCase().split("_"))
				{
					woodName += ( s.substring(0, 1).toUpperCase() + s.substring(1, s.length()) + " ");
				}
				
				translationBuilder.add(item, colorName + woodName + "Crib");
			}
		}
	}
}
