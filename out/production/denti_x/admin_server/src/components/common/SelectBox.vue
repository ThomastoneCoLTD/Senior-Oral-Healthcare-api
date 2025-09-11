<template>
  <v-select
    :id="id"
    v-model="selectedItem"
    :items="items"
    :menu-props="{ bottom: true, offsetY: true, closeOnClick: true }"
    background-color="inputBG"
    item-color="selectedBlue"
    :height="height"
    :placeholder="placeholder"
    :item-text="format.text"
    :item-value="format.value"
    dense
    outlined>
    <template #append>
      <i class="icon select_menu_icon"></i>
    </template>
  </v-select>
</template>

<script>
export default {
  name: 'SelectBox',
  props: {
    value: {
      type: [String, Number],
      default: '',
    },
    id: {
      type: String,
      default: '',
    },
    items: {
      type: Array,
      default: () => [],
    },
    height: {
      type: String,
      default: '48',
    },
    placeholder: {
      type: String,
      default: '',
    },
    format: {
      type: Object,
      default: () => {
        return {
          text: 'text',
          value: 'value',
        };
      },
    },
  },
  computed: {
    selectedItem: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit('fieldChanged', value);
        this.$emit('input', value);
      },
    },
  },
};
</script>

<style scoped></style>
