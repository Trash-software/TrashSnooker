import os


def count_file(rel_name: str, ext_list: list, base="") -> int:
	file_name = os.path.join(base, rel_name)
	file_count = 0
	line_count = 0
	try:
		if os.path.isdir(file_name):
			file_list = os.listdir(file_name)
			for f in file_list:
				count = count_file(f, ext_list, file_name)
				file_count += count[0]
				line_count += count[1]
		else:
			for ext in ext_list:
				if rel_name.endswith(ext):
					with open(file_name, "r", encoding="utf-8") as f:
						file_count += 1
						lines = [line.strip() for line in f.readlines()]
						for line in lines:
							if len(line) > 0:
								line_count += 1
					return 1, line_count
	except Exception as e:
		print(file_name + " " + str(e))
		return file_count, line_count
	return file_count, line_count


if __name__ == "__main__":
	ext_line = input("Extensions, separated by space: ")
	ext_list = [part.strip() for part in ext_line.split(" ")]
	
	total_count = count_file(os.getcwd(), ext_list)
	print(f"Total file count {total_count[0]}, total line count {total_count[1]}")

	input("Press ENTER to exit")
