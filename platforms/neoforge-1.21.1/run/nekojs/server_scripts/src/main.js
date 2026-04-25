// server example script
console.info('Hello, World! (Loaded server example script)');

ServerEvents.recipes(event => {

    // 1. 熔炉测试 (调用 smelting 方法)
    // 传入的 '#forge:raw_materials/iron' 会被自动 Adapter 转为 Ingredient
    // 传入的 'minecraft:iron_ingot' 会被自动 Adapter 转为 ItemStackWrapper
    // event.recipes.minecraft.smelting('#forge:raw_materials/iron', 'minecraft:iron_ingot', 0.7, 200);

    // 2. 有序合成测试 (调用 shaped 方法)
    event.recipes.minecraft.shaped('minecraft:diamond_sword', [
        ' D ',
        ' D ',
        ' S '
    ], {
        D: 'minecraft:diamond',
        S: 'minecraft:stick'
    });

    // // 3. 无序合成测试 (调用 shapeless 方法)
    // // 甚至支持带数量的产物：Item.of('minecraft:blaze_powder', 2)
    // event.recipes.minecraft.shapeless(Item.of('minecraft:blaze_powder', 2), [
    //     'minecraft:blaze_rod'
    // ]);
    //
    // // -----------------------------------------------------------------
    // // 🔮 4. 容错测试 (Fallback)
    // // 如果玩家写了一个你根本没注册过的方法，比如 blasting（高炉）
    // // 此时代码并不会报错！它会自动进入 FallbackProxy，并提示玩家必须传入 JSON。
    //
    // // 如果玩家这样写（报错拦截）：
    // // event.recipes.minecraft.blasting('A', 'B'); // 会在控制台报红字：“请传入标准 JSON 对象”
    //
    // // 如果玩家这样写（完美通过 Fallback 生成 JSON 配方）：
    // event.recipes.minecraft.blasting({
    //     ingredient: { item: "minecraft:iron_ore" },
    //     result: { id: "minecraft:iron_ingot" },
    //     experience: 0.1,
    //     cookingtime: 100
    // });
});