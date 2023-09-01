import json
import os

try:
    from scipy import stats
except ModuleNotFoundError:
    print("缺少运行需要的组件")
    print("请打开cmd或PowerShell，运行以下命令后再次运行本程序")
    input("python -m pip install scipy")
    exit(0)

BASE_LEVEL_PERK = 2
PERK_RANDOM_RANGE = (0.5, 2.0)


def perk_range(level: int):
    mean = BASE_LEVEL_PERK + level // 10
    low = max(1, round(PERK_RANDOM_RANGE[0] * mean))
    high = round(PERK_RANDOM_RANGE[1] * mean)
    return low, high


def get_africa(p_value):
    if p_value > 0.9:
        return "超级无敌大欧皇"
    elif p_value > 0.8:
        return "超级欧皇"
    elif p_value > 0.7:
        return "欧皇"
    elif p_value > 0.6:
        return "欧洲平民"
    elif p_value > 0.4:
        return "亚洲人"
    elif p_value > 0.3:
        return "北非人种"
    elif p_value > 0.2:
        return "小非酋"
    elif p_value > 0.1:
        return "终极非酋"
    else:
        return "吃我一矛"


def analyze(career_file):
    with open(career_file, "r", encoding="utf8") as jsf:
        js = json.load(jsf)
        careers = js["careers"]
        for career in careers:
            if career["human"]:
                if "levelAwards" in career:
                    level_awd = career["levelAwards"]
                    lows = 0
                    highs = 0
                    got = 0
                    prob_list = []
                    for level, awd in level_awd.items():
                        level = int(level)
                        rng = perk_range(level)
                        actual = awd["perks"]
                        prob_list.append((actual - rng[0]) / (rng[1] - rng[0]))

                        lows += rng[0]
                        highs += rng[1]
                        got += actual

                    # Bates distribution
                    n = len(prob_list)
                    value = sum(prob_list) / n
                    expect = 0.5
                    variance = 1 / (12 * n)
                    sd = variance ** 0.5
                    z_score = (value - expect) / sd
                    p = stats.norm.cdf(z_score)

                    print("==========")
                    print(f"{n}次升级记录在案，最少{lows}点，最多{highs}点，"
                          f"你获得了{got}点，欧气程度：{round(p * 100, 2)}%")
                    print("评价：" + get_africa(p))
                else:
                    print("0.4版本之前的升级记录不纳入统计")
                return
    print("未找到存档")


if __name__ == '__main__':
    print("请确认该文件已放置在游戏根目录或存档目录下")
    file_list = os.listdir()
    if "career.json" in file_list:
        analyze(career_file="career.json")
    else:
        try:
            car_list = os.listdir("user/career")
            num_car = {}
            for car in car_list:
                car_info = "user/career/" + car + "/career_info.ini"
                if os.path.exists(car_info):
                    with open(car_info, "r", encoding="utf8") as info:
                        for line in info.readlines():
                            line = line.strip()
                            spl = line.split("=")
                            if len(spl) == 2 and spl[0] == "name":
                                num_car[str(len(num_car) + 1)] = \
                                    spl[1], "user/career/" + car + "/career.json"
            for num, car in num_car.items():
                print(num, car[0])

            num_input = input("请输入球员编号")
            if num_input in num_car:
                analyze(num_car[num_input][1])
            else:
                print("找不到对象！")

        except FileNotFoundError:
            print("找不到存档目录")

    input("按ENTER退出")
