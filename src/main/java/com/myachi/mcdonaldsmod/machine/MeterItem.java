package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlockTags;
import com.myachi.mcdonaldsmod.energy.CableBlock;
import com.myachi.mcdonaldsmod.energy.EnergyNetwork;
import com.myachi.mcdonaldsmod.energy.EnergyNetworks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * 万用表：右键电缆看数据，分两个模式。
 *
 * <ul>
 *     <li><b>右键</b>：只报当前这一根电缆的情况（电压、电流、功率）；</li>
 *     <li><b>潜行右键</b>：报整张电网的情况（电压、电缆数、机器数、供电/需求/实际传输）。</li>
 * </ul>
 *
 * <p>数据都是上一次 tick 结算的结果，所以边走边点能看到实时变化。
 */
public class MeterItem extends Item {
    public MeterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        if (!world.getBlockState(pos).isIn(ModBlockTags.CABLE)) {
            return ActionResult.PASS;
        }
        if (world instanceof ServerWorld serverWorld && context.getPlayer() != null) {
            EnergyNetwork network = EnergyNetworks.get(serverWorld).getNetworkAt(pos);
            if (context.getPlayer().isSneaking()) {
                reportNetwork(context.getPlayer(), network);
            } else {
                reportCable(context.getPlayer(), network, pos, world);
            }
        }
        return ActionResult.SUCCESS;
    }

    /** 模式一：整张电网。 */
    private static void reportNetwork(PlayerEntity player, @Nullable EnergyNetwork network) {
        player.sendMessage(Text.translatable("item.mcdonalds-mod.meter.header").formatted(Formatting.AQUA), false);
        if (network == null) {
            player.sendMessage(Text.translatable("item.mcdonalds-mod.meter.no_network").formatted(Formatting.GRAY), false);
            return;
        }
        line(player, "item.mcdonalds-mod.meter.voltage", network.getVoltage() + " V");
        line(player, "item.mcdonalds-mod.meter.cables", network.getCableCount() + "");
        line(player, "item.mcdonalds-mod.meter.machines", network.getMachineCount() + "");
        // 网络里的功率是毫瓦，换算成瓦再交给格式化工具
        line(player, "item.mcdonalds-mod.meter.supply", MachineNumbers.power(network.getSupplyMilliWatts() / 1000L));
        line(player, "item.mcdonalds-mod.meter.demand", MachineNumbers.power(network.getDemandMilliWatts() / 1000L));
        line(player, "item.mcdonalds-mod.meter.delivered", MachineNumbers.power(network.getDeliveredMilliWatts() / 1000L));
        line(player, "item.mcdonalds-mod.meter.loss", MachineNumbers.power(network.getLossMilliWatts() / 1000L));
    }

    /** 模式二：当前这一根电缆。 */
    private static void reportCable(PlayerEntity player, @Nullable EnergyNetwork network, BlockPos pos, World world) {
        player.sendMessage(Text.translatable("item.mcdonalds-mod.meter.cable_header").formatted(Formatting.AQUA), false);
        line(player, "item.mcdonalds-mod.meter.position", pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        if (world.getBlockState(pos).getBlock() instanceof CableBlock cable) {
            line(player, "item.mcdonalds-mod.meter.rated",
                    cable.getVoltage() + " V / " + MachineNumbers.current(cable.getRatedCurrentMilliAmps()));
            line(player, "item.mcdonalds-mod.meter.resistance",
                    MachineNumbers.ohms(cable.getResistanceMilliOhms()) + " Ω/格");
        }
        if (network == null) {
            player.sendMessage(Text.translatable("item.mcdonalds-mod.meter.no_network").formatted(Formatting.GRAY), false);
            return;
        }
        int voltage = network.getVoltage();
        long current = network.getCableCurrentMilliAmps(pos);
        line(player, "item.mcdonalds-mod.meter.voltage", voltage + " V");
        String currentText = MachineNumbers.current((int) Math.min(Integer.MAX_VALUE, current));
        if (world.getBlockState(pos).getBlock() instanceof CableBlock cable && cable.getRatedCurrentMilliAmps() > 0) {
            int percent = (int) (current * 100L / cable.getRatedCurrentMilliAmps());
            currentText += " (" + percent + "%)";
        }
        line(player, "item.mcdonalds-mod.meter.cable_current", currentText);
        // 功率(mW) = 电压(V) * 电流(mA)
        line(player, "item.mcdonalds-mod.meter.cable_power", MachineNumbers.power(voltage * current / 1000L));
    }

    private static void line(PlayerEntity player, String key, String value) {
        player.sendMessage(Text.translatable(key).append(": ").append(value).formatted(Formatting.GRAY), false);
    }
}
