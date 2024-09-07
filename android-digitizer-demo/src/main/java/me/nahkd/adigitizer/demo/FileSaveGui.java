package me.nahkd.adigitizer.demo;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImString;

public class FileSaveGui {
	private ImString currentFolder = new ImString(1024);
	private ImString fileName;

	private FileEntry[] files = new FileEntry[0];

	public FileSaveGui(File initialFolder, ImString fileName) {
		this.fileName = fileName;
		setFolder(initialFolder);
	}

	public FileSaveGui(File initialFolder, String fileName) {
		this(initialFolder, new ImString(fileName, 255));
	}

	public void imgui(float width, float listHeight, Consumer<File> onSave) {
		if (ImGui.button("Refresh")) refreshFileList();
		ImGui.sameLine();
		ImGui.setNextItemWidth(width - ImGui.calcTextSize("Refresh").x - ImGui.calcTextSize("Go").x - 32f);
		ImGui.inputText("##Path", currentFolder);
		ImGui.sameLine();
		if (ImGui.button("Go")) setFolder(new File(currentFolder.get()));

		if (ImGui.beginTable("fileExplorer", 3, ImGuiTableFlags.ScrollY, width, listHeight)) {
			ImGui.tableSetupColumn("File name", ImGuiTableColumnFlags.WidthFixed, width - 120f);
			ImGui.tableSetupColumn("A", ImGuiTableColumnFlags.WidthFixed, 20f);
			ImGui.tableSetupColumn("Type", ImGuiTableColumnFlags.WidthFixed, 100f);
			ImGui.tableHeadersRow();

			for (FileEntry entry : files) {
				ImGui.tableNextRow();
				ImGui.tableSetColumnIndex(0);
				if (ImGui.selectable(entry.fileName) && entry.attributes.contains("D")) setFolder(entry.file);
				ImGui.tableSetColumnIndex(1);
				ImGui.text(entry.attributes);
				ImGui.tableSetColumnIndex(2);
				ImGui.text(entry.type);
			}

			ImGui.endTable();
		}

		ImGui.setCursorPosY(ImGui.getCursorPosY() + 20);
		ImGui.text("Save file as:");
		ImGui.setNextItemWidth(width - ImGui.calcTextSize("Save").x - 16f);
		ImGui.inputText("##SaveAs", fileName);
		ImGui.sameLine();
		if (ImGui.button("Save")) onSave.accept(new File(currentFolder.get(), fileName.get()));
	}

	public void setFolder(File folder) {
		try {
			folder = folder.getCanonicalFile();
		} catch (IOException e) {
			folder = folder.getAbsoluteFile();
		}

		currentFolder.set(folder.toString());
		refreshFileList();
	}

	public void refreshFileList() {
		File current = new File(currentFolder.get());
		File[] ls = current.listFiles();
		files = new FileEntry[ls.length + 1];
		files[0] = new FileEntry(new File(current, ".."), "..", "Parent", "D");
		for (int i = 0; i < ls.length; i++) files[i + 1] = new FileEntry(ls[i]);
	}

	static record FileEntry(File file, String fileName, String type, String attributes) {
		public FileEntry(File file) {
			this(file, file.getName(), file.isDirectory() ? "Folder" : "File", (file.isDirectory() ? "D" : "")
				+ (file.isHidden() ? "H" : ""));
		}
	}
}
