package net.adeptstack.cts.utils.screenUtils;

public class ToolTipUtils {
    public static String GetSoundName(int variant) {
        String name = switch (variant) {
            case 0 -> "gui.tracksta.tooltip.default_sound";
            case 1 -> "gui.tracksta.tooltip.no_sound";
            case 2 -> "gui.tracksta.tooltip.ice_sound";
            case 3 -> "gui.tracksta.tooltip.ic2_sound";
            case 4 -> "gui.tracksta.tooltip.desiro_hc_sound";
            case 5 -> "gui.tracksta.tooltip.stadlerflirt_sound";
            case 6 -> "gui.tracksta.tooltip.nyc_subway_sound";
            case 7 -> "gui.tracksta.tooltip.pkp_ic_sound";
            case 8 -> "gui.tracksta.tooltip.ic_sound";
            case 9 -> "gui.tracksta.tooltip.elevator_sound";
            case 10 -> "gui.tracksta.tooltip.london_1973_stock_soud";
            case 11 -> "gui.tracksta.tooltip.london_s7_stock_sound";
            case 12 -> "gui.tracksta.tooltip.london_overground_sound";
            case 13 -> "gui.tracksta.tooltip.railjet_sound";
            case 14 -> "gui.tracksta.tooltip.sbahn_sound";
            case 15 -> "gui.tracksta.tooltip.talent_sound";
            case 16 -> "gui.tracksta.tooltip.caf_urbos_sound";
            case 17 -> "gui.tracksta.tooltip.subway_sound";
            case 18 -> "gui.tracksta.tooltip.modern_ice_sound";
            case 19 -> "gui.tracksta.tooltip.class_350_sound";
            case 20 -> "gui.tracksta.tooltip.class_390_sound";
            case 21 -> "gui.tracksta.tooltip.class_450_sound";
            case 22 -> "gui.tracksta.tooltip.1996_stock_sound";
            default -> "gui.tracksta.tooltip.ic2_sound";
        };
        return name;
    }

    public static String GetNLPlatformBlockToolTipName(int variant) {
        int signBlockCount = 0;
        String name = "";

        for (int i = 0; i < variant; i++) {
            if (i % 5 == 0) {
                signBlockCount++;
            }
        }

        if (variant == 0) {
            name = "0";
        } else if (variant % 5 == 1) {
            name = signBlockCount + "";
        } else if (variant % 5 == 2) {
            name = signBlockCount + "A";
        } else if (variant % 5 == 3) {
            name = signBlockCount + "B";
        } else if (variant % 5 == 4) {
            name = signBlockCount + "C";
        } else if (variant % 5 == 0) {
            name = signBlockCount + "D";
        }
        return name;
    }

    public static String GetDEPlatformBlockToolTipName(int variant) {
        String name = switch (variant) {
            case 0 -> "0";
            case 17 -> "A";
            case 18 -> "B";
            case 19 -> "C";
            case 20 -> "D";
            case 21 -> "E";
            case 22 -> "F";
            case 23 -> "-";
            default -> "" + variant;
        };
        return name;
    }

    public static String GetCHPlatformBlockToolTipName(int variant) {
        String name = switch (variant) {
            case 0 -> "0";
            case 17 -> "A";
            case 18 -> "B";
            case 19 -> "C";
            case 20 -> "D";
            case 21 -> "E";
            case 22 -> "F";
            default -> variant + "";
        };
        return name;
    }
}

