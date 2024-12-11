// export interface PointInfo {
//     id: number;
//     type: 'charge' | 'use';
//     amount: number;
//     date: string;
//     description: string;
// }
export interface PointInfo {
  type: "REWARD" | "ITEM" | "GAME" | "CHARGING";
  changeCash: number;
  resultCash: number;
  createdAt: string;
}
