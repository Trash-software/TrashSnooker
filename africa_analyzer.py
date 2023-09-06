import json
import os
import random

try:
    from scipy import stats
    import matplotlib.pyplot as plt
except ModuleNotFoundError:
    print("缺少运行需要的组件")
    print("请打开cmd或PowerShell，运行以下命令后再次运行本程序")
    input("python -m pip install scipy")
    input("python -m pip install matplotlib")
    exit(0)

BASE_LEVEL_PERK = 2
PERK_RANDOM_RANGE = (0.5, 2.0)


def perk_range(level: int):
    mean = BASE_LEVEL_PERK + level // 10
    low = max(1, round(PERK_RANDOM_RANGE[0] * mean))
    high = round(PERK_RANDOM_RANGE[1] * mean)
    return low, high


def random_perk(p_range):
    avail_perks = list(range(p_range[0], p_range[1] + 1))
    return random.choice(avail_perks)


def simulate_one(level):
    return random_perk(perk_range(level))


def simulate_life(dst_level):
    level_perks = {}
    for lv in range(2, dst_level + 1):
        level_perks[lv] = simulate_one(lv)
    return level_perks


def simulate_multiple(n, dst_level):
    res = []
    for i in range(n):
        res.append(simulate_life(dst_level))
    return res


def simulate_distribution(n, dst_level):
    lives = simulate_multiple(n, dst_level)
    results = []
    for life in lives:
        d = analyze_dict(life)
        results.append(d)
    cum_avg = [d['avg'] for d in results]
    cum_p = [d['p'] for d in results]
    plt.subplot(1, 2, 1)
    plt.title("Percentage")
    plt.hist(cum_avg, bins=40, range=(0, 1))
    plt.subplot(1, 2, 2)
    plt.title("Europe")
    plt.hist(cum_p, range=(0, 1))
    plt.show()


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


def analyze_dict(level_perks):
    lows = 0
    highs = 0
    got = 0
    prob_list = []
    for level, perk in level_perks.items():
        rng = perk_range(level)
        prob_list.append((perk - rng[0] + 0.5) / (rng[1] - rng[0] + 1))

        lows += rng[0]
        highs += rng[1]
        got += perk

    n = len(prob_list)
    if n > 0:
        # Bates distribution
        avg = sum(prob_list) / n
        expect = 0.5
        variance = 1 / (12 * n)
        sd = variance ** 0.5
        z_score = (avg - expect) / sd
        p = stats.norm.cdf(z_score)
        return {"n": n, "lows": lows, "highs": highs, "got": got,
                "avg": avg, "sd": sd, "p": p}


def analyze(career_file):
    with open(career_file, "r", encoding="utf8") as jsf:
        js = json.load(jsf)
        careers = js["careers"]
        for career in careers:
            if career["human"]:
                if "levelAwards" in career:
                    level_awd = career["levelAwards"]
                    level_perks = {int(k): int(v["perks"]) for k, v in level_awd.items()}
                    res = analyze_dict(level_perks)
                    if res:
                        print("==========")
                        print(f"{res['n']}次升级记录在案，最少{res['lows']}点，最多{res['highs']}点，"
                              f"你获得了{res['got']}点")
                        print(f"平均每次抽奖获得了{round(res['avg'] * 100, 2)}%的点数"
                              f"（sd={round(res['sd'] * 100, 2)}%），"
                              f"欧气程度：{round(res['p'] * 100, 2)}%")
                        print("评价：" + get_africa(res['p']))
                    else:
                        print("0.4版本之前的升级记录不纳入统计")
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
            print("0", "模拟")
            for num, car in num_car.items():
                print(num, car[0])

            num_input = input("请输入球员编号")
            if num_input == "0":
                simulate_distribution(1000, 50)
                exit(0)
            elif num_input in num_car:
                analyze(num_car[num_input][1])
            else:
                print("找不到对象！")

        except FileNotFoundError:
            print("找不到存档目录")

    input("按ENTER退出")
