export interface IProvider {
  name: string;
  provider: string;
  description?: string;
  supported_model_types?: string[];
  enable?: boolean;
}

export interface IModel {
  model_id: string;
  name: string;
  provider: string;
  type: string;
  enable?: boolean;
}

export interface IModelSelectorItem {
  provider: IProvider;
  models: IModel[];
}
