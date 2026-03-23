# SMA* for the Labyrinth Problem

Implementation of the **SMA\* (Simplified Memory-Bounded A\*)** algorithm applied to pathfinding in a labyrinth, as a university project for the Artificial Intelligence course.

## What it does
Finds the shortest path between a start cell and a destination cell in a grid-based labyrinth, using a fixed memory limit. When memory is full, the least promising nodes are dropped and their best-known cost is backed up to the parent, allowing the algorithm to regenerate them later if needed.

## How it works
- The labyrinth is represented as a 2D grid where `1` = open cell and `0` = wall
- Each cell is scored using `f = max(parent.f, g + h)` where:
  - `g` = actual steps taken to reach the cell
  - `h` = Manhattan distance to the destination
- When memory is full, the shallowest node with the highest f value is dropped
- The dropped node's f value is backed up to its parent for potential regeneration

## Test cases
- **5x5 grid**: simple path, easy to trace
- **9x9 winding grid**: long winding path that triggers memory pressure
- **Impossible grid**: destination surrounded by walls, demonstrates failure case

## How to run
1. Clone the repository
2. Open in IntelliJ IDEA
3. Run `SMAStar.java`

## Authors
Preduț Alexia

Tanul Bianca-Elena
