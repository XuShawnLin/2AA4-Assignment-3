# Catan visualizer

This repository provides a Python visualization script, `light_visualizer.py`, for rendering a Catan board from JSON files.

---

## Overview

The visualizer reads JSON files of a Catan board and game state, and renders the board as an image.

### Input files
- `base_map.json` — defines the board layout.
- `state.json` — defines the game state (roads, buildings, etc.).
### Output files
- `scraped_boards/board0.png` — example output image.

---

## Setup instructions

### 1. Clone this repository
```bash
git clone https://github.com/ssm-lab/2aa4-2026-base.git
cd visualize
```
### 2. Create and activate a Python virtual environment
```bash
python3.11 -m venv .venv
source .venv/bin/activate
```

### 3. Install required dependencies
```bash
pip install -r requirements.txt
```

### 4. Clone the Catanatron repository
```bash
git clone -b gym-rendering https://github.com/bcollazo/catanatron.git
cd catanatron
```
### 5. Install dependencies for Catanatron
```bash
pip install -e ".[web,gym,dev]"
```
### 6. Return to the visualizer directory
```bash
cd ..
```

### 7. Run the script
The script supports two execution modes.
#### (1) Single-render mode
Render a single board image from a given state JSON file:
```bash
python light_visualizer.py base_map.json state.json
```
#### (2) Watch mode
Continuously watch ```state.json``` and re-render the board whenever the file changes:
```bash
python light_visualizer.py base_map.json --watch
```
